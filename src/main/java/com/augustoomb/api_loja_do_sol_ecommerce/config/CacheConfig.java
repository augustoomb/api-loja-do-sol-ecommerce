package com.augustoomb.api_loja_do_sol_ecommerce.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Configuração do cache Redis (Spring Cache + Spring Data Redis).
 *
 * Como funciona:
 * - {@link EnableCaching} ativa o processamento das anotações @Cacheable / @CacheEvict
 *   espalhadas pelos services (ex.: ProductService e CategoryService).
 * - O CacheManager criado aqui é o "motor" por trás dessas anotações: ele decide
 *   onde os dados ficam (Redis), como são serializados (JSON) e por quanto tempo
 *   ficam vivos (TTL).
 *
 * Uso conservador: só leituras do catálogo (produtos/categorias) são cacheadas.
 * Dados de carrinho, pedidos e estoque continuam sendo lidos sempre do banco.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    /**
     * Cria o CacheManager baseado no Redis.
     *
     * - cacheDefaults: config aplicada a TODOS os caches (aqui, 2 minutos).
     * - withCacheConfiguration: sobrescreve a config para um cache específico
     *   (categorias mudam raramente, então podem viver mais tempo: 10 minutos).
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig(Duration.ofMinutes(2)))
                .withCacheConfiguration("categories", cacheConfig(Duration.ofMinutes(10)))
                .build();
    }

    private RedisCacheConfiguration cacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)                              // tempo de vida das chaves no Redis
                .disableCachingNullValues()                 // nunca guarda "null" (economiza chaves inúteis)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(RedisSerializer.string()))  // chaves legíveis: products::5
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(RedisSerializer.json()));   // valores em JSON (não serialização binária do Java)
    }

    /**
     * Degradação graciosa: se o Redis estiver fora do ar, qualquer operação de
     * cache falha, MAS a aplicação continua respondendo normalmente — o erro é
     * apenas registrado no log e a consulta segue direto ao PostgreSQL.
     *
     * Sem isso, uma falha do Redis derrubaria todas as requisições que passam
     * por um metodo anotado com @Cacheable.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache '{}' indisponível ao LER a chave '{}': {}. Consultando o banco normalmente.",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache '{}' indisponível ao GRAVAR a chave '{}': {}. Dado não será cacheado.",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache '{}' indisponível ao INVALIDAR a chave '{}': {}. A próxima leitura pode estar defasada.",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache '{}' indisponível ao LIMPAR: {}.", cache.getName(), exception.getMessage());
            }
        };
    }
}

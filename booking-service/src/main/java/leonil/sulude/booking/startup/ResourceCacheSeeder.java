package leonil.sulude.booking.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Triggers cache seeding once the application is fully ready.
 *
 * Delegates the actual retry-protected work to ResourceCacheSeedAttempter — a separate
 * bean, deliberately. See that class's javadoc for why this split is required: Spring's
 * @Retryable/@Recover only work across a real bean boundary, not on a self-invoked method
 * within the same class.
 *
 * Uses ApplicationReadyEvent rather than @PostConstruct — @PostConstruct runs during bean
 * initialisation, before Eureka registration is complete and before Feign clients are
 * configured, so a call to the Catalog Service at that point would fail regardless of
 * Catalog's own availability. ApplicationReadyEvent fires only after the full context is
 * ready, Eureka has registered the service, and the embedded server is accepting connections.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceCacheSeeder {

    private final ResourceCacheSeedAttempter seedAttempter;

    @EventListener(ApplicationReadyEvent.class)
    public void seedResourceCache() {
        log.info("Seeding resource cache from Catalog Service...");
        seedAttempter.attemptSeed(); // external bean call — Retry/Recover proxy applies correctly here
    }
}
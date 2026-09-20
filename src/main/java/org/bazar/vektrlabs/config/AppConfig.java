package org.bazar.vektrlabs.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
        "org.bazar.vektrlabs.entity",
        "org.jericho.common.entity",
        "org.jericho.mediaresource.entity",
        "org.jericho.notifications.entity",
        "org.jericho.ai.entity"
})
@EnableJpaRepositories(basePackages = {
        "org.bazar.vektrlabs.repository",
        "org.jericho.mediaresource.repository",
        "org.jericho.notifications.repository"
})
@ComponentScan(basePackages = {
        "org.jericho.mediaresource",
        "org.jericho.payment",
        "org.jericho.notifications",
//        "org.jericho.ai"
})
@EnableCaching
public class AppConfig {
}
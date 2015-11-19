/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo;

import com.mongodb.Mongo;
import cz.jirutka.spring.embedmongo.EmbeddedMongoBuilder;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoEmbeddedConfiguration {

    @Bean(destroyMethod = "close")
    public Mongo mongo() throws IOException, InterruptedException {
        return new EmbeddedMongoBuilder()
                .version("2.6.5")
                .bindIp("127.0.0.1")
                .port(37017)
                .build();
    }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsInstanceOf;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.LazyLoadingProxy;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author jllach
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = Application.class)
public class SpringTest {

    private static final MongodStarter starter = MongodStarter.getDefaultInstance();
    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongoD;
    private static MongoClient mongo;
    
    @Autowired
    private MongoTemplate template;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        int port = 12345;
        IMongodConfig mongodConfig = new MongodConfigBuilder().version(Version.Main.PRODUCTION)
                                                              .net(new Net(port, Network.localhostIsIPv6())).build();
        mongodExecutable = starter.prepare(mongodConfig);
        mongoD = mongodExecutable.start();
        mongo = new MongoClient("localhost", port);
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        mongoD.stop();
        mongodExecutable.stop();
    }
    
    
    @Test
    public void testStackOverflowBecauseOfCyclicRelationBetweenDocumentsLazyDbRefRelated() {
        One one = new One("one");
        Two two = new Two("two");
        template.save(one);
        template.save(two);

        one.refToTwo = two;
        two.refToOne = one;
        template.save(one);
        template.save(two);

        One foundOne = template.findOne(Query.query(Criteria.where("id").is(one.id)), One.class);
        Assert.assertThat(foundOne.refToTwo, Matchers.notNullValue());
        Assert.assertThat(foundOne.refToTwo.id, Matchers.is("two"));
        Assert.assertThat(foundOne.refToTwo.getRefToOne(), IsInstanceOf.instanceOf(LazyLoadingProxy.class));
        Assert.assertThat(foundOne.refToTwo.getRefToOne().id, Matchers.is("one"));

        Two foundTwo = template.findOne(Query.query(Criteria.where("id").is(two.id)), Two.class);
        Assert.assertThat(foundTwo.refToOne, Matchers.notNullValue());
        Assert.assertThat(foundTwo.refToOne, IsInstanceOf.instanceOf(LazyLoadingProxy.class));
        Assert.assertThat(foundTwo.refToOne.id, Matchers.is("one"));
        Assert.assertThat(foundTwo.refToOne.refToTwo,  Matchers.nullValue());
        Assert.assertThat(foundTwo.refToOne.getRefToTwo(), Matchers.notNullValue());
        Assert.assertThat(foundTwo.refToOne.getRefToTwo().id, Matchers.is("two"));
    }
    
    @Test
    public void testOKBecauseOfNoCyclicRelationBetweenDocumentsLazyDbRefRelated() {
        One one = new One("one");
        Two two = new Two("two");
        template.save(one);
        template.save(two);

        one.refToTwo = two;
        template.save(one);

        One foundOne = template.findOne(Query.query(Criteria.where("id").is(one.id)), One.class);
        Assert.assertThat(foundOne.refToTwo, Matchers.notNullValue());
        Assert.assertThat(foundOne.refToTwo, IsInstanceOf.instanceOf(Two.class));
        Assert.assertThat(foundOne.refToTwo.id, Matchers.is("two"));
        Assert.assertThat(foundOne.refToTwo.refToOne, Matchers.nullValue());

        Two foundTwo = template.findOne(Query.query(Criteria.where("id").is(two.id)), Two.class);
        Assert.assertThat(foundTwo.refToOne, Matchers.nullValue());
    }
    
    @Test
    public void testStackOverflowBecauseOfCyclicRelationBetweenDocumentsDbRefRelated () {
        Three three = new Three("three");
        Four four   = new Four("four");
        template.save(three);
        template.save(four);

        three.refToFour = four;
        four.refToThree = three;
        template.save(three);
        template.save(four);

        Three foundThree = template.findOne(Query.query(Criteria.where("id").is(three.id)), Three.class);
        Assert.assertThat(foundThree.refToFour, Matchers.notNullValue());
        Assert.assertThat(foundThree.refToFour, IsInstanceOf.instanceOf(Four.class));
        Assert.assertThat(foundThree.refToFour.id, Matchers.is("four"));
        Assert.assertThat(foundThree.refToFour.refToThree, IsInstanceOf.instanceOf(Three.class));
        Assert.assertThat(foundThree.refToFour.refToThree.id, Matchers.is("three"));

        Four foundFour = template.findOne(Query.query(Criteria.where("id").is(four.id)), Four.class);
        Assert.assertThat(foundFour.refToThree, Matchers.notNullValue());
        Assert.assertThat(foundFour.refToThree, IsInstanceOf.instanceOf(Three.class));
        Assert.assertThat(foundFour.refToThree.id, Matchers.is("three"));
        Assert.assertThat(foundFour.refToThree.refToFour,  IsInstanceOf.instanceOf(Four.class));
        Assert.assertThat(foundFour.refToThree.refToFour.id, Matchers.is("four"));
    }
    
    @Test
    public void testOKBecauseOfNoCyclicRelationBetweenDocumentsDbRefRelated () {
        Three three = new Three("three");
        Four four   = new Four("four");
        template.save(three);
        template.save(four);

        three.refToFour = four;
        template.save(three);

        Three foundThree = template.findOne(Query.query(Criteria.where("id").is(three.id)), Three.class);
        Assert.assertThat(foundThree.refToFour, Matchers.notNullValue());
        Assert.assertThat(foundThree.refToFour, IsInstanceOf.instanceOf(Four.class));
        Assert.assertThat(foundThree.refToFour.id, Matchers.is("four"));
        Assert.assertThat(foundThree.refToFour.refToThree, Matchers.nullValue());

        Four foundFour = template.findOne(Query.query(Criteria.where("id").is(four.id)), Four.class);
        Assert.assertThat(foundFour.refToThree, Matchers.nullValue());
    }

    //
    // with ANY kind of dbref ... when using the 2 arg constructor --> stackoverflow
    //                      but when using single argument constructor everything works fine
    //
    
    public static class One {
      @Id 
      private String id;
      @org.springframework.data.mongodb.core.mapping.DBRef(lazy = true) 
      private Two refToTwo;
      
//        @PersistenceConstructor
        public One(String id) {
            this.id = id;
        }
        @PersistenceConstructor
        public One(String id, Two refToTwo) {
            this.id = id;
            this.refToTwo = refToTwo;
        }
        public String getId() {
            return id;
        }
        public Two getRefToTwo() {
            return refToTwo;
        }
    }

    public static class Two {
      @Id 
      private String id;
      @org.springframework.data.mongodb.core.mapping.DBRef(lazy = true) 
      private One refToOne;

//        @PersistenceConstructor
        public Two(String id) {
            this.id = id;
        }
        @PersistenceConstructor
        public Two(String id, One refToOne) {
            this.id = id;
            this.refToOne = refToOne;
        }
        public String getId() {
            return id;
        }
        public One getRefToOne() {
            return refToOne;
        }
    }
    
    //
    // the same problem with regular DBRefs
    //
    
    public static class Three {
        @Id 
        private String id;
        @org.springframework.data.mongodb.core.mapping.DBRef 
        private Four refToFour;
        
//        @PersistenceConstructor
        public Three(String id) {
            this.id = id;
        }
        @PersistenceConstructor
        public Three(String id, Four refToFour) {
            this.id = id;
            this.refToFour = refToFour;
        }
        public String getId() {
            return id;
        }
        public Four getRefToFour() {
            return refToFour;
        }
    }
    
    public static class Four {
        @Id 
        private String id;
        @org.springframework.data.mongodb.core.mapping.DBRef 
        private Three refToThree;
        
//        @PersistenceConstructor
        public Four(String id) {
            this.id = id;
        }
        @PersistenceConstructor
        public Four(String id, Three refToThree) {
            this.id = id;
            this.refToThree = refToThree;
        }
        public String getId() {
            return id;
        }
        public Three getRefToThree() {
            return refToThree;
        }
    }
}
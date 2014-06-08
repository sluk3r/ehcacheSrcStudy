/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statistics;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Result;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Statistic;
import net.sf.ehcache.statistics.extended.ExtendedStatisticsImpl;
import net.sf.ehcache.util.RetryAssert;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsCollectionContaining;
import org.hamcrest.core.IsEqual;
import org.junit.Test;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.archive.Timestamped;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static net.sf.ehcache.CacheOperationOutcomes.GetOutcome.HIT;
import static net.sf.ehcache.CacheOperationOutcomes.GetOutcome.MISS_EXPIRED;
import static net.sf.ehcache.CacheOperationOutcomes.GetOutcome.MISS_NOT_FOUND;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

/**
 *
 * @author cdennis
 */
public class ExtendedStatisticsTest {
    
    @Test(expected = IllegalStateException.class)
    public void testExtendedStatisticsWithoutRequiredStats() {
        new ExtendedStatisticsImpl(new StatisticsManager(), 
                Executors.newSingleThreadScheduledExecutor(), 10, TimeUnit.SECONDS,
                StatisticsGateway.DEFAULT_HISTORY_SIZE,
                StatisticsGateway.DEFAULT_INTERVAL_SECS,
                StatisticsGateway.DEFAULT_SEARCH_INTERVAL_SECS);
    }

    @Test
    public void testSimpleCacheStatistics() throws InterruptedException {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);
            
            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();
            
            assertThat(extendedStats.get().component(HIT).count().value(), is(0L));
            assertThat(extendedStats.get().component(MISS_NOT_FOUND).count().value(), is(0L));
            assertThat(extendedStats.get().component(MISS_EXPIRED).count().value(), is(0L));
            assertThat(extendedStats.size().value(), IsEqual.<Number>equalTo(0));
            foo.get("miss");
            
            assertThat(extendedStats.get().component(HIT).count().value(), is(0L));
            assertThat(extendedStats.get().component(MISS_NOT_FOUND).count().value(), is(1L));
            assertThat(extendedStats.get().component(MISS_EXPIRED).count().value(), is(0L));
            assertThat(extendedStats.size().value(), IsEqual.<Number>equalTo(0));
            
            foo.put(new Element("miss", "miss"));
            foo.get("miss");
            
            assertThat(extendedStats.get().component(HIT).count().value(), is(1L));
            assertThat(extendedStats.get().component(MISS_NOT_FOUND).count().value(), is(1L));
            assertThat(extendedStats.get().component(MISS_EXPIRED).count().value(), is(0L));
            assertThat(extendedStats.size().value(), IsEqual.<Number>equalTo(1));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testCASReplace1CacheStatistics() throws InterruptedException {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);

            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();

            foo.put(new Element("one", "one"));

            assertThat(extendedStats.replaceOneArg().component(CacheOperationOutcomes.ReplaceOneArgOutcome.SUCCESS)
                .count().value(), is(0L));
            assertThat(extendedStats.replaceOneArg().component(CacheOperationOutcomes.ReplaceOneArgOutcome.FAILURE)
                .count().value(), is(0L));

            foo.replace(new Element("two", "two"));

            assertThat(extendedStats.replaceOneArg().component(CacheOperationOutcomes.ReplaceOneArgOutcome.SUCCESS)
                .count().value(), is(0L));
            assertThat(extendedStats.replaceOneArg().component(CacheOperationOutcomes.ReplaceOneArgOutcome.FAILURE)
                .count().value(), is(1L));

            foo.replace(new Element("one", "two"));

            assertThat(extendedStats.replaceOneArg().component(CacheOperationOutcomes.ReplaceOneArgOutcome.SUCCESS)
                .count().value(), is(1L));
            assertThat(extendedStats.replaceOneArg().component(CacheOperationOutcomes.ReplaceOneArgOutcome.FAILURE)
                .count().value(), is(1L));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testCASReplace2CacheStatistics() throws InterruptedException {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);

            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();

            foo.put(new Element("one", "one"));

            assertThat(extendedStats.replaceTwoArg().component(CacheOperationOutcomes.ReplaceTwoArgOutcome.SUCCESS)
                .count().value(), is(0L));
            assertThat(extendedStats.replaceTwoArg().component(CacheOperationOutcomes.ReplaceTwoArgOutcome.FAILURE)
                .count().value(), is(0L));

            foo.replace(new Element("one", "two"),new Element("one","two"));

            assertThat(extendedStats.replaceTwoArg().component(CacheOperationOutcomes.ReplaceTwoArgOutcome.SUCCESS)
                .count().value(), is(0L));
            assertThat(extendedStats.replaceTwoArg().component(CacheOperationOutcomes.ReplaceTwoArgOutcome.FAILURE)
                .count().value(), is(1L));

            foo.replace(new Element("one", "one"), new Element("one", "two"));

            assertThat(extendedStats.replaceTwoArg().component(CacheOperationOutcomes.ReplaceTwoArgOutcome.SUCCESS)
                .count().value(), is(1L));
            assertThat(extendedStats.replaceTwoArg().component(CacheOperationOutcomes.ReplaceTwoArgOutcome.FAILURE)
                .count().value(), is(1L));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testCASPutIfAbsentCacheStatistics() throws InterruptedException {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);

            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();

            foo.put(new Element("one", "one"));

            assertThat(extendedStats.putIfAbsent().component(CacheOperationOutcomes.PutIfAbsentOutcome.SUCCESS)
                .count().value(), is(0L));
            assertThat(extendedStats.putIfAbsent().component(CacheOperationOutcomes.PutIfAbsentOutcome.FAILURE)
                .count().value(), is(0L));

            foo.putIfAbsent(new Element("one","two"));
            assertThat(extendedStats.putIfAbsent().component(CacheOperationOutcomes.PutIfAbsentOutcome.SUCCESS)
                .count().value(), is(0L));
            assertThat(extendedStats.putIfAbsent().component(CacheOperationOutcomes.PutIfAbsentOutcome.FAILURE)
                .count().value(), is(1L));

            foo.putIfAbsent(new Element("two","one"));
            assertThat(extendedStats.putIfAbsent().component(CacheOperationOutcomes.PutIfAbsentOutcome.SUCCESS)
                .count().value(), is(1L));
            assertThat(extendedStats.putIfAbsent().component(CacheOperationOutcomes.PutIfAbsentOutcome.FAILURE)
                .count().value(), is(1L));
        } finally {
            manager.shutdown();
        }
    }


    @Test
    public void testCASRemoveElementCacheStatistics() throws InterruptedException {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);

            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();

            foo.put(new Element("one", "one"));

            assertThat(extendedStats.removeElement().component(CacheOperationOutcomes.RemoveElementOutcome.SUCCESS)
                .count().value(), is(0L));
            assertThat(extendedStats.removeElement().component(CacheOperationOutcomes.RemoveElementOutcome.FAILURE)
                .count().value(), is(0L));

            foo.removeElement(new Element("one", "one1"));

            assertThat(extendedStats.removeElement().component(CacheOperationOutcomes.RemoveElementOutcome.SUCCESS)
                .count().value(), is(0L));
            assertThat(extendedStats.removeElement().component(CacheOperationOutcomes.RemoveElementOutcome.FAILURE)
                .count().value(), is(1L));

            foo.removeElement(new Element("one", "one"));

            assertThat(extendedStats.removeElement().component(CacheOperationOutcomes.RemoveElementOutcome.SUCCESS)
                .count().value(), is(1L));
            assertThat(extendedStats.removeElement().component(CacheOperationOutcomes.RemoveElementOutcome.FAILURE)
                .count().value(), is(1L));

        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testExtendedCacheTimeToDisable() throws InterruptedException {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);
            
            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();
            extendedStats.setTimeToDisable(2, TimeUnit.SECONDS);
            Result missNotFound = extendedStats.get().component(MISS_NOT_FOUND);
            
            assertThat(missNotFound.count().active(), is(false));
            assertThat(missNotFound.rate().active(), is(false));
            assertThat(missNotFound.count().value(), is(0L));
            assertThat(missNotFound.rate().value(), is(0.0));
            
            foo.get("miss");
            assertThat(missNotFound.count().active(), is(false));
            assertThat(missNotFound.rate().active(), is(true));
            assertThat(missNotFound.count().value(), is(1L));
            assertThat(missNotFound.rate().value(), greaterThan(0.0));
 
            TimeUnit.SECONDS.sleep(1);
            
            foo.get("miss");
            assertThat(missNotFound.count().active(), is(false));
            assertThat(missNotFound.rate().active(), is(true));
            assertThat(missNotFound.count().value(), is(2L));
            assertThat(missNotFound.rate().value(), greaterThan(0.0));
            
            assertThat(missNotFound.count().active(), is(false));
            RetryAssert.assertBy(4, TimeUnit.SECONDS, isActive(missNotFound.rate()), is(false));
            
            foo.get("miss");
            assertThat(missNotFound.count().value(), is(3L));
            assertThat(missNotFound.rate().value(), is(0.0));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testExtendedCacheEnable() throws InterruptedException {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);
            
            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();
            extendedStats.setTimeToDisable(2, TimeUnit.SECONDS);
            extendedStats.setAlwaysOn(true);
            Result missNotFound = extendedStats.get().component(MISS_NOT_FOUND);            
            assertThat(missNotFound.count().active(), is(true));
            assertThat(missNotFound.rate().active(), is(true));
            
            assertThat(missNotFound.count().value(), is(0L));
            assertThat(missNotFound.rate().value(), is(0.0));
            
            foo.get("miss");
            assertThat(missNotFound.count().value(), is(1L));
            assertThat(missNotFound.rate().value(), greaterThan(0.0));

            TimeUnit.SECONDS.sleep(1);
            assertThat(missNotFound.count().active(), is(true));
            assertThat(missNotFound.rate().active(), is(true));
            
            foo.get("miss");
            assertThat(missNotFound.count().value(), is(2L));
            assertThat(missNotFound.rate().value(), greaterThan(0.0));
            
            TimeUnit.SECONDS.sleep(3);
            assertThat(missNotFound.count().active(), is(true));
            assertThat(missNotFound.rate().active(), is(true));
            
            foo.get("miss");
            assertThat(missNotFound.count().value(), is(3L));
            assertThat(missNotFound.rate().value(), greaterThan(0.0));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testExtendedCacheDisable() throws InterruptedException {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);
            
            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();
            extendedStats.setTimeToDisable(2, TimeUnit.SECONDS);
            extendedStats.setAlwaysOn(true);
            Result missNotFound = extendedStats.get().component(MISS_NOT_FOUND);
            assertThat(missNotFound.count().active(), is(true));
            assertThat(missNotFound.rate().active(), is(true));
            
            assertThat(missNotFound.count().value(), is(0L));
            assertThat(missNotFound.rate().value(), is(0.0));
            
            foo.get("miss");
            assertThat(missNotFound.count().value(), is(1L));
            assertThat(missNotFound.rate().value(), greaterThan(0.0));

            TimeUnit.SECONDS.sleep(1);
            assertThat(missNotFound.count().active(), is(true));
            assertThat(missNotFound.rate().active(), is(true));
            
            foo.get("miss");
            assertThat(missNotFound.count().value(), is(2L));
            assertThat(missNotFound.rate().value(), greaterThan(0.0));
            
            TimeUnit.SECONDS.sleep(3);
            assertThat(missNotFound.count().active(), is(true));
            assertThat(missNotFound.rate().active(), is(true));
            
            extendedStats.setAlwaysOn(false);
            
            RetryAssert.assertBy(4, TimeUnit.SECONDS, isActive(missNotFound.count()), is(false));
            RetryAssert.assertBy(4, TimeUnit.SECONDS, isActive(missNotFound.rate()), is(false));
            
            foo.get("miss");
            assertThat(missNotFound.count().value(), is(3L));
            assertThat(missNotFound.rate().value(), is(0.0));
        } finally {
            manager.shutdown();
        }
    }
    
    @Test
    public void testExtendedCacheLatencyMeasurement() {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);
            
            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();
            extendedStats.setAlwaysOn(true);
            Result missNotFound = extendedStats.get().component(MISS_NOT_FOUND);
            
            assertThat(missNotFound.count().value(), is(0L));
            assertThat(missNotFound.latency().average().value(), is(Double.NaN));
            
            foo.get("miss");
            assertThat(missNotFound.count().value(), is(1L));
            assertThat(missNotFound.latency().average().value(), greaterThan(0.0));
            assertThat(missNotFound.latency().minimum().value(), is(missNotFound.latency().maximum().value()));
            assertThat(missNotFound.latency().minimum().value().doubleValue(), is(missNotFound.latency().average().value()));

            foo.get("miss");
            assertThat(missNotFound.count().value(), is(2L));
            assertThat(missNotFound.latency().average().value(), greaterThan(0.0));
            assertThat(missNotFound.latency().minimum().value().doubleValue(), lessThanOrEqualTo(missNotFound.latency().average().value()));
            assertThat(missNotFound.latency().maximum().value().doubleValue(), greaterThanOrEqualTo(missNotFound.latency().average().value()));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testPassThroughHistory() throws InterruptedException {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);
            
            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();

            RetryAssert.assertBy(5L, TimeUnit.SECONDS, history(extendedStats.size()), IsCollectionContaining.<Timestamped<Number>>hasItem(ExtendedStatisticsTest.<Number>sample(Is.<Number>is(0))));
            foo.put(new Element("foo", "foo"));
            RetryAssert.assertBy(5L, TimeUnit.SECONDS, history(extendedStats.size()), IsCollectionContaining.<Timestamped<Number>>hasItem(ExtendedStatisticsTest.<Number>sample(Is.<Number>is(1))));
        } finally {
            manager.shutdown();
        }
    }

    static <T extends Number> Callable<List<Timestamped<T>>> history(final Statistic<T> statistic) {
      return new Callable<List<Timestamped<T>>>() {

        @Override
        public List<Timestamped<T>> call() throws Exception {
          return statistic.history();
        }
      };
    }
    
    static Callable<Boolean> isActive(final Statistic<?> stat) {
      return new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          return stat.active();
        }
      };
    }
    
    static <T> Matcher<Timestamped<T>> sample(final Matcher<? super T> matcher) {
      return new TypeSafeMatcher<Timestamped<T>>() {

        @Override
        protected boolean matchesSafely(Timestamped<T> t) {
          return matcher.matches(t.getSample());
        }

        @Override
        public void describeTo(Description d) {
          d.appendText(" contains sample ").appendDescriptionOf(matcher);
        }
      };
    }
}

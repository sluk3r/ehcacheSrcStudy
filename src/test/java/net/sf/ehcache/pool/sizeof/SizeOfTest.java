package net.sf.ehcache.pool.sizeof;

import static net.sf.ehcache.pool.sizeof.JvmInformation.CURRENT_JVM_INFORMATION;
import static net.sf.ehcache.pool.sizeof.JvmInformation.UNKNOWN_32_BIT;
import static net.sf.ehcache.pool.sizeof.JvmInformation.UNKNOWN_64_BIT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assume.assumeThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.Proxy;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import org.junit.Assert;
import javax.xml.datatype.DatatypeConstants;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Alex Snaps
 */
public class SizeOfTest extends AbstractSizeOfTest {

  public Object[] container;

  private static long deepSizeOf(SizeOf sizeOf, Object... obj) {
    return sizeOf.deepSizeOf(1000, true, obj).getCalculated();
  }

  @BeforeClass
  public static void setup() {
    System.err.println("java.vm.name:\t" + System.getProperty("java.vm.name", ""));
    System.err.println("java.vm.vendor:\t" + System.getProperty("java.vm.vendor", ""));
    assumeThat(System.getProperty("os.name"), not(containsString("AIX")));
    deepSizeOf(new CrossCheckingSizeOf(), null);
    System.err.println("JVM identified as: " + JvmInformation.CURRENT_JVM_INFORMATION);
    if (JvmInformation.CURRENT_JVM_INFORMATION == UNKNOWN_64_BIT || JvmInformation.CURRENT_JVM_INFORMATION == UNKNOWN_32_BIT) {
      System.getProperties().list(System.err);
    }
  }

  private final Collection<AssertionError> sizeOfFailures = new LinkedList<AssertionError>();

  @Test
  public void testSizeOf() throws Exception {
    Assume.assumeThat(CURRENT_JVM_INFORMATION.getMinimumObjectSize(), is(CURRENT_JVM_INFORMATION.getObjectAlignment()));

    SizeOf sizeOf = new CrossCheckingSizeOf();
    Assert.assertThat(deepSizeOf(sizeOf, TimeUnit.SECONDS), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, Object.class), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, 1), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, BigInteger.ZERO), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, BigDecimal.ZERO), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, MathContext.UNLIMITED), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, Locale.ENGLISH), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, Logger.global), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, Collections.EMPTY_SET), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, Collections.EMPTY_LIST), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, Collections.EMPTY_MAP), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, String.CASE_INSENSITIVE_ORDER), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, System.err), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, Proxy.NO_PROXY), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, CodingErrorAction.REPORT), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, DatatypeConstants.DAYS), is(0L));
    Assert.assertThat(deepSizeOf(sizeOf, DatatypeConstants.TIME), is(0L));
    
    assertThat(sizeOf.sizeOf(new Object()), "sizeOf(new Object())");
    assertThat(sizeOf.sizeOf(new Integer(1)), "sizeOf(new Integer(1))");
    assertThat(sizeOf.sizeOf(1000), "sizeOf(1000)");
    assertThat(deepSizeOf(sizeOf, new SomeClass(false)), "deepSizeOf(new SomeClass(false))");
    assertThat(deepSizeOf(sizeOf, new SomeClass(true)), "deepSizeOf(new SomeClass(true))");
    assertThat(sizeOf.sizeOf(new Object[] { }), "sizeOf(new Object[] { })");
    assertThat(sizeOf.sizeOf(new Object[] { new Object(), new Object(), new Object(), new Object() }), "sizeOf(new Object[] { new Object(), new Object(), new Object(), new Object() })");
    assertThat(sizeOf.sizeOf(new int[] { }), "sizeOf(new int[] { })");
    assertThat(sizeOf.sizeOf(new int[] { 987654, 876543, 765432, 654321 }), "sizeOf(new int[] { 987654, 876543, 765432, 654321 })");
    assertThat(deepSizeOf(sizeOf, new Pair(null, null)), "deepSizeOf(new Pair(null, null))");
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), null)), "deepSizeOf(new Pair(new Object(), null))");
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), new Object())), "deepSizeOf(new Pair(new Object(), new Object()))");
    assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), "deepSizeOf(new ReentrantReadWriteLock())");
    
    if (!sizeOfFailures.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (AssertionError e : sizeOfFailures) {
        sb.append(e.toString()).append('\n');
      }
      Assert.fail(sb.toString());
    }

    List<Object> list1 = new ArrayList<Object>();
    List<Object> list2 = new ArrayList<Object>();

    Object someInstance = new Object();
    list1.add(someInstance);
    list2.add(someInstance);

    Assert.assertThat(deepSizeOf(sizeOf, list1), is(deepSizeOf(sizeOf, list2)));
    Assert.assertThat(deepSizeOf(sizeOf, list1, list2) < (deepSizeOf(sizeOf, list1) + deepSizeOf(sizeOf, list2)), is(true));
    list2.add(new Object());
    Assert.assertThat(deepSizeOf(sizeOf, list2) > deepSizeOf(sizeOf, list1), is(true));
  }

  private void assertThat(Long size, String expression) {
    try {
      Assert.assertThat(expression, size, is(SizeOfTestValues.get(expression)));
    } catch (AssertionError e) {
      sizeOfFailures.add(e);
    }
  }

  @Test
  public void testOnHeapConsumption() throws Exception {
    SizeOf sizeOf = new CrossCheckingSizeOf();

    int size = 80000;
    int perfectMatches = 0;
    for (int j = 0; j < 5; j++) {
      container = new Object[size];
      long usedBefore = measureMemoryUse();
      for (int i = 0; i < size; i++) {
        container[i] = new EvilPair(new Object(), new SomeClass(i % 2 == 0));
      }

      long mem = 0;
      for (Object s : container) {
        mem += deepSizeOf(sizeOf, s);
      }

      long used = measureMemoryUse() - usedBefore;
      float percentage = 1 - (mem / (float) used);
      System.err.println("Run # " + (j + 1) + ": Deviation of " + String.format("%.3f", percentage * -100) +
                 "%\n" + used +
                 " bytes are actually being used, while we believe " + mem + " are");
      if (j > 1) {
        Assert.assertThat("Run # " + (j + 1) + ": Deviation of " + String.format("%.3f", percentage * -100) +
                   "% was above the +/-1.5% delta threshold \n" + used +
                   " bytes are actually being used, while we believe " + mem + " are (" +
                   (used - mem) / size + ")",
            Math.abs(percentage) < .015f, is(true));
      }
      if(used == mem && ++perfectMatches > 1) {
        System.err.println("Two perfect matches, that's good enough... bye y'all!");
        break;
      }
    }
  }

  private long measureMemoryUse() throws InterruptedException {
    long total;
    long freeAfter;
    long freeBefore;
    Runtime runtime = Runtime.getRuntime();
    do {
        total = runtime.totalMemory();
        freeBefore = runtime.freeMemory();
        System.gc();
        Thread.sleep(100);
        freeAfter = runtime.freeMemory();
    } while (total != runtime.totalMemory() || freeAfter > freeBefore);
    return total - freeAfter;
  }

  public static class SomeClass {

    public Object ref;

    public SomeClass(final boolean init) {
      if (init) {
        ref = new Object();
      }
    }
  }

  public static class Pair {
    private final Object one;
    private final Object two;

    public Pair(final Object one, final Object two) {
      this.one = one;
      this.two = two;
    }
  }

  public static final class Stupid {

    public static class internal {
      private int  someValue;
      private long otherValue;
    }

    internal internalVar   = new internal();
    int      someValue;
    long     someOther;
    long     otherValue;
    boolean  bool;
  }

  public static final class EvilPair extends Pair {

    private static final AtomicLong counter = new AtomicLong(Long.MIN_VALUE);

    private final Object oneHidden;
    private final Object twoHidden;
    private final Object copyOne;
    private final Object copyTwo;
    private final long   instanceNumber;

    private EvilPair(final Object one, final Object two) {
      super(one, two);
      instanceNumber = counter.getAndIncrement();
      if (instanceNumber % 4 == 0) {
        oneHidden = new Object();
        twoHidden = new Object();
      } else {
        oneHidden = null;
        twoHidden = null;
      }
      this.copyOne = one;
      this.copyTwo = two;
    }
  }
}

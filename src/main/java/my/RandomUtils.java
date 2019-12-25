package my;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import my.exceptions.ApplicationRuntimeException;
import my.web.utils.WebUtils;

/**
 * 
 * @author stephan
 *
 * @see Proper use of SecureRandom:
 *      https://www.cigital.com/justice-league-blog/2009
 *      /08/14/proper-use-of-javas-securerandom/
 *
 */
@Slf4j
public enum RandomUtils {
	INSTANCE;

	private SecureRandom rand;
	private byte[] tmp;
	private AtomicInteger callsCountToNextSeeding;

	private RandomUtils() {
		try {
			rand = SecureRandom.getInstance("SHA1PRNG", "SUN");
			tmp = rand.generateSeed(4);
			rand.nextBytes(tmp);
			callsCountToNextSeeding = new AtomicInteger(generateRandomCallsCount());
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new ApplicationRuntimeException(e);
		}
	}

	private int generateRandomCallsCount() {
		return rand.nextInt(100) + 1;
	}

	/**
	 * 
	 * Return a random value between {@code min} and {@code max} (inclusive).
	 * 
	 * @param min
	 * @param max
	 * @return
	 * 
	 * @throw IllegalArgumentException If {@code max} is strictly lower than
	 *        {@code min}.
	 * @see WebUtils#randLong(long, long)
	 */
	public static int randInt(int min, int max) {
		if (max < min) {
			throw new IllegalArgumentException("The max value MUST be greater than the min value. (actually min=" + min + ", max=" + max + ")");
		}

		return getInstance().getInternalSecureRandom().nextInt((max - min) + 1) + min;
	}

	/**
	 * 
	 * Return a random value between {@code min} and {@code max} (inclusive).
	 * 
	 * @param min
	 * @param max
	 * @return
	 * 
	 * @throw IllegalArgumentException If {@code max} is strictly lower than
	 *        {@code min}.
	 * @see WebUtils#randInt(int, int)
	 */
	public static long randLong(long min, long max) {
		if (max < min) {
			throw new IllegalArgumentException("The max value MUST be greater than the min value. (actually min=" + min + ", max=" + max + ")");
		}

		return nextLong((max - min) + 1) + min;
	}

	/**
	 * 
	 * @param n
	 * @return
	 * 
	 * @see http://stackoverflow.com/a/2546186/363573
	 */
	private static long nextLong(long n) {
		Random rng = getInstance().getInternalSecureRandom();

		// TODO: error checking and 2^x checking removed for simplicity.
		long bits, val;
		do {
			bits = (rng.nextLong() << 1) >>> 1;
			val = bits % n;
		} while (bits - val + (n - 1) < 0L);
		return val;
	}

	public static Random getSecureRandom() {
		return getInstance().getInternalSecureRandom();
	}

	private Random getInternalSecureRandom() {
		if (callsCountToNextSeeding.decrementAndGet() == 0) {
			callsCountToNextSeeding.set(generateRandomCallsCount());
			rand.setSeed(System.currentTimeMillis());
		}

		return rand;
	}

	/**
	 * 
	 * Suspend the execution of the current thread for a random value between
	 * {@code min} and {@code max} , both inclusive, expressed in {@code tu}.
	 * 
	 * @param min
	 * @param max
	 * @param tu
	 */
	public static void pauseRandomly(long min, long max, TimeUnit tu) {
		if (tu == null) {
			throw new IllegalArgumentException("The passed timeunit cannot be null.");
		}

		long randomPause = RandomUtils.randLong(min, max);
		try {
			log.info("Pausing execution for {} {}", randomPause, tu.toString());
			tu.sleep(randomPause);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApplicationRuntimeException(e);
		}
	}
	
	private static RandomUtils getInstance() {
		return INSTANCE;
	}
}

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.render.DrawContext;

import java.util.Random;

/**
 * Handles expiration after some amount of time has passed. Expiration time is computed at random betwenn a min and max
 * delay time.
 *
 * @version $Id: TimedExpirySupport.java 12784 2009-11-10 04:42:21Z tgaskins $
 * @deprecated Use of this class incurs a Globe dependency, which makes the associated objects unsharable among
 * different globes.
 */
public class TimedExpirySupport
{
    protected boolean expired = true;
    protected long expiryTime = -1L;
    protected int minExpiryTime = 1000;
    protected int maxExpiryTime = 2000;
    protected static Random rand = new Random();
    protected Object globeStateKey;

    /**
     * Creates a new expiry support object with the given min and max expiration delays.
     *
     * @param minExpiryTime the minimum time allowed to pass before expiration - milliseconds.
     * @param maxExpiryTime the maximum time allowed to pass before expiration - milliseconds.
     */
    public TimedExpirySupport(int minExpiryTime, int maxExpiryTime)
    {
        this.maxExpiryTime = minExpiryTime;
        this.maxExpiryTime = maxExpiryTime;
    }

    public void setExpired(boolean expired)
    {
        this.expired = expired;
    }

    public boolean isExpired(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.expired)
            return true;

        if (dc.getFrameTimeStamp() >= this.expiryTime)
            return true;

        //noinspection RedundantIfStatement
        if (this.globeStateKey != null && !this.globeStateKey.equals(dc.getGlobe().getStateKey(dc)))
            return true;

        return false;
    }

    public void updateExpiryCriteria(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.expiryTime = dc.getFrameTimeStamp() + this.minExpiryTime
            + rand.nextInt(this.maxExpiryTime - this.minExpiryTime);
        this.globeStateKey = dc.getGlobe().getStateKey(dc);
        this.expired = false;
    }
}

package net.simonvt.cathode.api.entity;

import net.simonvt.cathode.api.enumeration.RatingMode;

public class Viewing {

  private Ratings ratings;

  private Shouts shouts;

  public static class Ratings {

    private RatingMode mode;
  }

  public static class Shouts {

    private boolean showBadges;

    private boolean showSpoilers;

    public boolean getShowBadges() {
      return showBadges;
    }

    public boolean getShowSpoilers() {
      return showSpoilers;
    }
  }
}

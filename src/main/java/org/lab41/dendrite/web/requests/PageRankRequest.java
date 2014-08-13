package org.lab41.dendrite.web.requests;

public class PageRankRequest {
    private double alpha = 0.15d;

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }
}

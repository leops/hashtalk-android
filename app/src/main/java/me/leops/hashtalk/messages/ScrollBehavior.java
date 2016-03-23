package me.leops.hashtalk.messages;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by leops on 23/03/2016.
 */

public class ScrollBehavior extends AppBarLayout.ScrollingViewBehavior {

    private SearchView sView;

    public ScrollBehavior() {
    }

    public ScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return
            dependency instanceof Snackbar.SnackbarLayout ||
            super.layoutDependsOn(parent, child, dependency);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        boolean result = super.onDependentViewChanged(parent, child, dependency);

        int paddingBottom = child.getTop() - sView.getHeight();
        if(dependency instanceof Snackbar.SnackbarLayout) {
            paddingBottom += dependency.getHeight() - ViewCompat.getTranslationY(dependency);
        }

        child.setPadding(
            child.getPaddingLeft(),
            child.getPaddingTop(),
            child.getPaddingRight(),
            paddingBottom
        );

        return result;
    }

    public void setSearchView(SearchView sView) {
        this.sView = sView;
    }
}

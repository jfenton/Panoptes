package com.jfenton.panoptes;

import android.database.CursorWindow;

public class PanoptesCardCursorWindow extends CursorWindow {
    private boolean mIsClosed = false;
    public PanoptesCardCursorWindow(boolean localWindow) {
        super (localWindow);
    }
}
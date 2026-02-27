package com.github.dev.tool.plugins.counter;

import java.awt.*;

/**
 * FlowLayout subclass that fully supports wrapping of components.
 * Unlike FlowLayout, WrapLayout computes preferred size based on the
 * actual container width, allowing components to wrap to the next line
 * inside scroll panes and resizable containers.
 */
class WrapLayout extends FlowLayout {
    private static final long serialVersionUID = 1L;

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;

            // Walk up the parent chain to find a meaningful width constraint
            if (targetWidth == 0) {
                Container parent = target.getParent();
                while (parent != null) {
                    int pw = parent.getSize().width;
                    if (pw > 0) {
                        Insets pi = parent.getInsets();
                        targetWidth = pw - pi.left - pi.right;
                        break;
                    }
                    parent = parent.getParent();
                }
            }

            if (targetWidth <= 0) {
                targetWidth = Integer.MAX_VALUE;
            }


            Insets insets = target.getInsets();
            int hgap = getHgap();
            int vgap = getVgap();
            int maxWidth = targetWidth - insets.left - insets.right - hgap * 2;

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int count = target.getComponentCount();
            for (int i = 0; i < count; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    // Wrap to next row if needed
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        dim.width = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + vgap;
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth > 0) {
                        rowWidth += hgap;
                    }
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            // Add last row
            dim.width = Math.max(dim.width, rowWidth);
            dim.height += rowHeight;

            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.top + insets.bottom + vgap * 2;

            return dim;
        }
    }
}


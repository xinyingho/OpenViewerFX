/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/support/
 *
 * (C) Copyright 1997-2015 IDRsolutions and Contributors.
 *
 * This file is part of JPedal/JPDF2HTML5
 *
     This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


 *
 * ----------------
 * IncQuadTree.java
 * ----------------
 */
package com.idrsolutions.image.jpeg2000;

/**
 *
 */
public class IncQuadTree {

    private final TreeNode[] nodeList;
    private int currentLevel;

    public IncQuadTree(int width, int height, int def) {
        final int x = Math.max(width, height);
        int n = 1, tt = 0;
        while (x > n) {
            n <<= 1;
            tt++;
        }
        final int levLen = tt + 1;

        nodeList = new TreeNode[levLen];

        for (int i = 0; i < levLen; i++) {
            final TreeNode lev = new TreeNode();
            lev.width = width;
            lev.height = height;
            final int dim = width * height;
            for (int j = 0; j < dim; j++) {
                lev.items.put(j, def);
            }
            nodeList[i] = lev;
            width = width%2 == 0 ? width/2 : (width+1)/2;
            height = height%2 == 0 ? height/2 : (height+1)/2;
        }
    }

    public boolean reset(int m, int n, int stop) {
        int curLev = 0;
        final int len = nodeList.length;
        while (curLev < len) {
            final TreeNode level = nodeList[curLev];
            final int index = m + n * level.width;
            level.index = index;
            final int val = level.items.get(index);

            if (val == 0xff) {
                break;
            }
            if (val > stop) {
                currentLevel = curLev;
                updateValues();
                return false;
            }
            m >>= 1;
            n >>= 1;
            curLev++;
        }

        currentLevel = curLev - 1;
        return true;
    }

    public void incrementValue(int stop) {
        final TreeNode level = nodeList[currentLevel];
        level.items.put(level.index, stop + 1);
        updateValues();
    }

    private void updateValues() {
        int levIndex = currentLevel;
        TreeNode level = nodeList[levIndex];
        final int curVal = level.items.get(level.index);
        while (--levIndex >= 0) {
            level = nodeList[levIndex];
            level.items.put(level.index, curVal);
        }
    }

    public boolean nextNode() {
        int curLev = currentLevel;
        TreeNode level = nodeList[curLev];
        final int value = level.items.get(level.index);
        level.items.put(level.index, 0xff);
        curLev--;
        if (curLev < 0) {
            return false;
        }
        currentLevel = curLev;
        level = nodeList[curLev];
        level.items.put(level.index, value);
        return true;
    }

}

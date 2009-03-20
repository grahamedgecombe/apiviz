/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @author tags. See the COPYRIGHT.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.apiviz;

/**
 *
 * @author bsneade
 */
public enum ColorCombination {

    red(Color.red, Color.red4),
    blue(Color.skyblue, Color.royalblue3),
    green(Color.palegreen, Color.springgreen4),
    orange(Color.orange, Color.orangered3),
    brown(Color.rosybrown2, Color.peru),
    purple(Color.slateblue, Color.purple3),
    yellow(Color.yellow, Color.yellow4),
    grey(Color.grey95, Color.grey51);

    private Color fillColor;
    private Color lineColor;

    private ColorCombination(Color fillColor, Color lineColor) {
        this.fillColor = fillColor;
        this.lineColor = lineColor;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }

}

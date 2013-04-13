/*
Copyright (C) 2012 Qweex
This file is a part of Callisto.

Callisto is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

Callisto is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Callisto; If not, see <http://www.gnu.org/licenses/>.
*/
package com.qweex.utils;

/** A small exception class for when the parsing stops part-way through an episode in the XML. */
public class UnfinishedParseException extends Exception
{
	private static final long serialVersionUID = 2068788493309925174L;
	private String thatWhichWasWrong;
 
    public UnfinishedParseException(String thatWhichWasWrong){
        this.thatWhichWasWrong = thatWhichWasWrong;
    }
 
    public String toString(){
        return "Reached end before: " + thatWhichWasWrong + (thatWhichWasWrong.equals("Title") ? ", but that's ok" : "");
    }
}
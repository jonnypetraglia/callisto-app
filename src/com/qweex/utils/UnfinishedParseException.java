/*
 * Copyright (C) 2012-2014 Qweex
 * This file is a part of Callisto.
 *
 * Callisto is free software; it is released under the
 * Open Software License v3.0 without warranty. The OSL is an OSI approved,
 * copyleft license, meaning you are free to redistribute
 * the source code under the terms of the OSL.
 *
 * You should have received a copy of the Open Software License
 * along with Callisto; If not, see <http://rosenlaw.com/OSL3.0-explained.htm>
 * or check OSI's website at <http://opensource.org/licenses/OSL-3.0>.
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
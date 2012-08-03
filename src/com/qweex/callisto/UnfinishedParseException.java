package com.qweex.callisto;

//This is a small class for when the parsing stops part-way through an episode
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
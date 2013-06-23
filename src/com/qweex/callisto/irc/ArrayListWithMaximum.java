package com.qweex.callisto.irc;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: notbryant
 * Date: 6/22/13
 * Time: 8:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ArrayListWithMaximum<E> extends ArrayList<E>
{
    private int maximum;

    public void setMaximumCapacity(int m)
    {
        maximum=m;
        if(size()>maximum)
            this.removeRange(maximum, this.size());
    }

    public int getMaximumCapacity()
    {
        return maximum;
    }

    @Override
    public boolean add(E object)
    {
        if(this.size()+1==maximum)
            this.remove(this.size()-1);
        return super.add(object);
    }

    @Override
    public void add(int index, E object)
    {
        if(this.size()+1==maximum)
            this.remove(this.size()-1);
        super.add(index, object);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection)
    {
        if(this.size()+collection.size()==maximum)
            this.removeRange(this.size()-collection.size(), this.size());
        return super.addAll(collection);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> collection)
    {
        if(this.size()+collection.size()==maximum)
            this.removeRange(this.size()-collection.size(), this.size());
        return super.addAll(index, collection);
    }
}

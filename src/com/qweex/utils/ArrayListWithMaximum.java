/*
        DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                    Version 2, December 2004

 Copyright (C) 2013 Jon Petraglia <MrQweex@qweex.com>

 Everyone is permitted to copy and distribute verbatim or modified
 copies of this license document, and changing it is allowed as long
 as the name is changed.

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. You just DO WHAT THE FUCK YOU WANT TO.
 */
package com.qweex.utils;

import java.util.ArrayList;
import java.util.Collection;

/** An ArrayList that has a maximum; when the maximum is reached, it is trimmed */

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

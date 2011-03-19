/* Copyright 2011 GOTO Metrics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.gotometrics.hbase.rowkey;

import java.io.IOException;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class RowKeyTestCase
{
  protected RowKey key;

  public abstract RowKey createRowKey();

  public abstract Object createObject();

  public abstract int compareTo(Object o1, Object o2);

  @Before
  public void setUp() {
    key = createRowKey();
  }

  @After
  public void tearDown() {
    key = null;
  }

  public RowKeyTestCase setRowKey(RowKey key) { this.key = key; return this; }


  public void serialize(Object o, ImmutableBytesWritable w) 
    throws IOException 
  {
    key.serialize(o, w);
  }

  public Object deserialize(ImmutableBytesWritable w) throws IOException {
    return key.deserialize(w);
  }

  public ImmutableBytesWritable allocateBuffer(Object o) 
    throws IOException
  {
    return new ImmutableBytesWritable(new byte[key.getSerializedLength(o)]);
  }

  public void assertBoundsEquals(ImmutableBytesWritable w, int offset, 
      int len)
  {
    assertEquals("Offset corrupt", w.getOffset(), offset);
    assertEquals("Length corrupt", w.getLength(), len);
  }

  public void testSerialization(Object o, ImmutableBytesWritable w) 
    throws IOException 
  {
    int origOffset = w.getOffset(),
        origLength = w.getLength(),
       expectedLength = key.getSerializedLength(o);

    serialize(o, w);
    assertBoundsEquals(w, origOffset + expectedLength, 
        origLength - expectedLength);

    w.set(w.get(), origOffset, origLength);
    Object p = deserialize(w);

    assertEquals("Data corrupt", compareTo(o, p), 0);
    assertBoundsEquals(w, origOffset + expectedLength, 
        origLength - expectedLength);
    w.set(w.get(), origOffset, origLength);
  }

  public void testSkip(Object o, ImmutableBytesWritable w) 
    throws IOException
  {
    int origOffset = w.getOffset(),
        origLength = w.getLength(),
        expectedLength = key.getSerializedLength(o);
    key.skip(w);
    assertBoundsEquals(w, origOffset + expectedLength, 
        origLength - expectedLength);
    w.set(w.get(), origOffset, origLength);
  }

  public void testSort(Object o1, ImmutableBytesWritable w1, Object o2, 
      ImmutableBytesWritable w2) throws IOException
  {
    int expectedOrder = compareTo(o1, o2),
        byteOrder = Integer.signum(Bytes.compareTo(w1.get(), w1.getOffset(), 
              w1.getLength(), w2.get(), w2.getOffset(), w2.getLength()));
    if (key.getOrder() == Order.DESCENDING) 
      expectedOrder = -expectedOrder;
    assertEquals("Invalid sort order", expectedOrder, byteOrder);
  }

  @Test
  public void testRowKey() throws IOException {
    Object o1 = createObject(),
           o2 = createObject();
    ImmutableBytesWritable w1 = allocateBuffer(o1),
                           w2 = allocateBuffer(o2);
    testSerialization(o1, w1);
    testSerialization(o2, w2);
    testSkip(o1, w1);
    testSkip(o2, w2);
    testSort(o1, w1, o2, w2);
  }
}

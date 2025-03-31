/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2025 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.xpdustry.avs.service.providers.type;

import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.util.network.Subnet;

import arc.files.Fi;
import arc.struct.Seq;


/** @implNote work in progress, don't use it. */
public abstract class LocalFileProvider extends AddressProvider implements ProviderCategories.Cacheable {
  protected Seq<AddressValidity> cache = new Seq<>(false);
  /** The file containing address ranges. */
  protected Fi file;

  public LocalFileProvider(String name) { super(name); }
  public LocalFileProvider(String name, String displayName) { super(name, displayName); }
  
  @Override
  public AddressValidity get(String subnet) {
    return cache.find(a -> a.subnet.toString().equals(subnet));
  }

  @Override
  public AddressValidity get(Subnet subnet) {
    return cache.find(a -> a.subnet.equals(subnet));
  }
  
  @Override
  public Seq<AddressValidity> matches(String address) {
    return cache.select(s -> s.subnet.isInNet(address));
  }
  
  @Override
  public Seq<AddressValidity> getCache() {
    return cache.copy();
  }
  
  @Override
  public Seq<Subnet> list() {
    return cache.map(a -> a.subnet);
  }

  @Override
  public int cacheSize() {
    return cache.size;
  } 

  @Override
  protected boolean loadMiscSettings() {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  protected boolean reloadMiscSettings() {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  protected boolean saveMiscSettings() {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public void checkAddressImpl(AddressProviderReply reply) {
    reply.setResult(cache.find(v -> v.subnet.isInNet(reply.address)));
  }
  

  protected abstract Seq<Subnet> loadFile(Fi file);
}

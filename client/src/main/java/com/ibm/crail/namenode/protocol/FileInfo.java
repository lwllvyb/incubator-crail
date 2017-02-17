/*
 * Crail: A Multi-tiered Distributed Direct Access File System
 *
 * Author: Patrick Stuedi <stu@zurich.ibm.com>
 *
 * Copyright (C) 2016, IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.ibm.crail.namenode.protocol;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.crail.CrailNodeType;
import com.ibm.crail.conf.CrailConstants;

public class FileInfo {
	public static final int CSIZE = 44;
	
	private long fd;
	protected AtomicLong capacity;
	private CrailNodeType type;
	private long dirOffset;
	private long token;
	private long modificationTime;
	
	public FileInfo(){
		this(-1, CrailNodeType.DATAFILE);
	}
	
	protected FileInfo(long fd, CrailNodeType type){
		this.fd = fd;
		this.type = type;
		this.dirOffset = 0;
		this.capacity = new AtomicLong(0);
		this.token = 0;
		this.modificationTime = 0;
	}
	
	public void setFileInfo(FileInfo fileInfo){
		this.fd = fileInfo.getFd();
		this.type = fileInfo.getType();
		this.dirOffset = fileInfo.getDirOffset();
		
		this.capacity.set(fileInfo.getCapacity());
		this.token = fileInfo.getToken();
		this.modificationTime = fileInfo.getModificationTime();
	}
	
	public int write(ByteBuffer buffer, boolean shipToken){
		buffer.putLong(fd);
		buffer.putLong(capacity.get());
		buffer.putInt(type.value());
		buffer.putLong(dirOffset);
		if (shipToken){
			buffer.putLong(token);
		} else {
			buffer.putLong(0);
		}
		buffer.putLong(modificationTime);
		
		return CSIZE;
	}
	
	public void update(ByteBuffer buffer) throws UnknownHostException{
		fd = buffer.getLong();
		capacity.set(buffer.getLong());
		type = CrailNodeType.parse(buffer.getInt());
		dirOffset = buffer.getLong();
		token = buffer.getLong();
		modificationTime = buffer.getLong();
	}
	
	public long getCapacity() {
		return capacity.get();
	}
	
	public long setCapacity(long newcapacity) {
		long oldcapacity = capacity.get();
		if (newcapacity > oldcapacity){
			capacity.compareAndSet(oldcapacity, newcapacity);
			this.setModificationTime(System.currentTimeMillis());
		}
		return capacity.get();
	}
	
	public long incCapacity(int delta) {
		long newcapacity = capacity.addAndGet(delta);
		this.setModificationTime(System.currentTimeMillis());
		return newcapacity;
	}	
	
	public void resetCapacity(){
		capacity.set(0);		
	}	
	
	public long getToken() {
		return token;
	}

	public void resetToken(){
		token = 0;
	}

	public void updateToken(){
		if (!type.isDir()){
			this.token = System.nanoTime() + TimeUnit.SECONDS.toNanos(CrailConstants.TOKEN_EXPIRATION);
		}
	}

	public long getModificationTime() {
		return modificationTime;
	}

	public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}

	public long getDirOffset() {
		return dirOffset;
	}

	protected void setDirOffset(long dirOffset) {
		this.dirOffset = dirOffset;
	}

	public long getFd() {
		return fd;
	}

	public String toString() {
		return "fd " + fd + ", capacity " + capacity + ", type " + type.value() + ", dirOffset " + dirOffset + ", token " + token;
	}

	public CrailNodeType getType(){
		return type;
	}

	public boolean tokenFree(){
		return System.nanoTime() > token;
	}
}

/*
 * Copyright (c) 2011-2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.ipc.netty.udp;

import java.util.function.Consumer;
import javax.annotation.Nullable;

import io.netty.bootstrap.Bootstrap;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.Connection;
import reactor.ipc.netty.ConnectionObserver;
import reactor.ipc.netty.channel.BootstrapHandlers;

/**
 * @author Stephane Maldini
 */
final class UdpClientLifecycle extends UdpClientOperator implements ConnectionObserver {

	final Consumer<? super Bootstrap>       onConnect;
	final Consumer<? super Connection>      onConnected;
	final Consumer<? super Connection>      onDisconnected;

	UdpClientLifecycle(UdpClient server,
			@Nullable Consumer<? super Bootstrap> onConnect,
			@Nullable Consumer<? super Connection> onConnected,
			@Nullable Consumer<? super Connection> onDisconnected) {
		super(server);
		this.onConnect = onConnect;
		this.onConnected = onConnected;
		this.onDisconnected = onDisconnected;
	}

	@Override
	public Bootstrap configure() {
		Bootstrap b = source.configure();
		ConnectionObserver observer = BootstrapHandlers.connectionObserver(b);
		BootstrapHandlers.connectionObserver(b, observer.then(this));
		return b;
	}

	@Override
	public Mono<? extends Connection> connect(Bootstrap b) {
		if (onConnect != null) {
			return source.connect(b)
			             .doOnSubscribe(s -> onConnect.accept(b));
		}
		return source.connect(b);
	}

	@Override
	public void onStateChange(Connection connection, ConnectionObserver.State newState) {
		if (newState == ConnectionObserver.State.CONFIGURED) {
			if (onConnected != null) {
				onConnected.accept(connection);
			}
			if (onDisconnected != null) {
				connection.onDispose(() -> onDisconnected.accept(connection));
			}
			return;
		}
	}
}
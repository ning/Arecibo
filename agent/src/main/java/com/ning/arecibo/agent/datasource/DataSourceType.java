package com.ning.arecibo.agent.datasource;

public enum DataSourceType {
    HTTPResponseCheck,
    TCPConnectCheck,
	JMX,
	JMXComposite,
	JMXOperationInvocation,
	SNMP,
	Tracer
}

package com.ning.arecibo.event.transport;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import com.ning.arecibo.eventlogger.Event;


public class EventSerializerUDPUtil
{
	public static byte[] toByteArray(EventSerializer ser, Event event) throws IOException
	{
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ser.serialize(event, bout);
		return bout.toByteArray();
	}

	public static Event fromByteArray(EventSerializer ser, byte[] buf) throws IOException
	{
		ByteArrayInputStream bin = new ByteArrayInputStream(buf);
		return ser.deserialize(bin);
	}

	public static Event fromUDPPacket(byte b[], Map<String, EventSerializer> serializers) throws IOException
	{
		short headerLen = (short) (((b[1] & 0xFF) << 0) + ((b[0]) << 8));
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(b, 2, headerLen)));
		String line = null;
		Map<String, String> headers = new HashMap<String, String>();
		while ((line = br.readLine()) != null) {
			String h[] = line.split(":");
			if (h != null && h.length == 2) {
				String key = h[0].trim();
				String value = h[1].trim();
				headers.put(key, value);
			}
		}
        String contentType = headers.get(EventSerializer.HEADER_CONTENT_TYPE);
        if (contentType != null) {
            EventSerializer serializer = serializers.get(contentType);
            if ( serializer != null ) {
                Event event = serializer.deserialize(new ByteArrayInputStream(b, headerLen + 2, b.length - headerLen - 2));
                return event;
            }
        }
        throw new IOException("unknown content type, no serializer found!");
    }

	public static byte[] toUDPPacket(Map<String, String> headers, Event event, EventSerializer serializer) throws IOException
	{
		byte[] payload = toByteArray(serializer, event);
		StringWriter sw = new StringWriter();
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			sw.write(String.format("%s: %s\n", entry.getKey(), entry.getValue()));
		}
		byte[] top = sw.toString().getBytes() ;
		byte[] packet = new byte[top.length + payload.length + 2];

		packet[1] = (byte) (top.length >>> 0);
		packet[0] = (byte) (top.length >>> 8);

		System.arraycopy(top, 0, packet, 2, top.length);
		System.arraycopy(payload, 0, packet, top.length + 2, payload.length);

		return packet ;
	}

}

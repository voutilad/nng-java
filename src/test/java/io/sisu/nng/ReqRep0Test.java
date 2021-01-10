package io.sisu.nng;

import com.sun.jna.Pointer;
import io.sisu.nng.internal.*;
import io.sisu.nng.jna.Size;
import io.sisu.nng.reqrep0.Rep0Socket;
import io.sisu.nng.reqrep0.Req0Socket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ReqRep0Test {

    private static NngLibrary lib;

    @BeforeAll
    public static void beforeAll() {
        lib = Nng.lib();
    }
    public void check(int rv) {
        if (rv == 0) {
            return;
        }

        final String err = lib.nng_strerror(rv);
        Assertions.fail(err);
    }

    @Test
    public void CanSendAndReceive() throws Exception {
        final String url = String.format("inproc://%s",
                new Throwable().getStackTrace()[0].getMethodName());

        Socket req = new Req0Socket();
        Socket rep = new Rep0Socket();

        rep.listen(url);
        req.dial(url);

        Message msg = new Message();
        msg.append("hey man".getBytes(StandardCharsets.UTF_8));
        req.sendMessage(msg);
        Assertions.assertFalse(msg.isValid());

        Message msg2 = rep.receiveMessage();
        Assertions.assertTrue(msg2.isValid());
        Assertions.assertEquals("hey man",
                Charset.defaultCharset().decode(msg2.getBody()).toString());
    }

    @Test
    public void CanSendAndReceiveLowLevel() {
        final String url = String.format("inproc://%s",
                new Throwable().getStackTrace()[0].getMethodName());
        SocketStruct reqPtr = new SocketStruct();
        SocketStruct repPtr = new SocketStruct();

        check(lib.nng_rep0_open(repPtr));
        check(lib.nng_req0_open(reqPtr));

        SocketStruct.ByValue req = new SocketStruct.ByValue(reqPtr);
        SocketStruct.ByValue rep = new SocketStruct.ByValue(repPtr);

        check(lib.nng_listen(rep, url, Pointer.NULL, 0));
        check(lib.nng_dial(req, url, Pointer.NULL, 0));

        MessageByReference msgRef = new MessageByReference();
        check(lib.nng_msg_alloc(msgRef, new Size(0)));
        MessagePointer msg = msgRef.getMessage();

        final String payload = "Peace be the journey";
        ByteBuffer buffer = ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8));
        check(lib.nng_msg_append(msg, buffer, new Size(buffer.limit())));

        check(lib.nng_sendmsg(req, msg, 0));

        MessageByReference msgRef2 = new MessageByReference();
        check(lib.nng_recvmsg(rep, msgRef2, 0));
        MessagePointer msg2 = msgRef2.getMessage();

        int len = lib.nng_msg_len(msg2);
        Assertions.assertTrue(len > 0);
        BodyPointer body = lib.nng_msg_body(msg2);
        Assertions.assertNotEquals(Pointer.NULL, body.getPointer());
        String data = body.getPointer().getString(0, StandardCharsets.UTF_8.name());
        Assertions.assertEquals(payload, data);

        check(lib.nng_msg_free(msg));
        check(lib.nng_msg_free(msg2));

        check(lib.nng_close(req));
        check(lib.nng_close(rep));
    }
}

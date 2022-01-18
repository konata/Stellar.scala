package playground.susi;


public class Susi {
    static boolean likely = false;

    public static boolean likely() {
        return likely;
    }

    public static boolean unlikely() {
        return !likely;
    }


    public static void main(String[] args) {
        Sources emailSource = new EmailSource();
        Sources usernameSource = new UsernameSource();
        Sources credentialSource = new CredentialSource();


        Sink emailSink = new EmailSink();
        Sink usernameSink = new UsernameSink();
        Sink credentialSink = new CredentialSink();

        emailSink = likely ? emailSink : usernameSink;

        Sink _emailSink = emailSource.transformTo(usernameSink, credentialSink);
        Sink _usernameSink = usernameSource.transformTo(emailSink, credentialSink);
        Sink _credentialSink = credentialSource.transformTo(emailSink, usernameSink);
    }

}

abstract class Sources {
    Sink transformTo(Sink alternative1, Sink alternative2) {
        if (Susi.likely()) {
            return alternative1;
        }
        return alternative2;
    }
}

class EmailSource extends Sources {
    Sink transformTo(Sink alternative1, Sink alternative2) {
        if (Susi.likely()) {
            return alternative1;
        }
        return alternative2;
    }

}

class UsernameSource extends Sources {
    Sink transformTo(Sink alternative1, Sink alternative2) {
        if (Susi.unlikely()) {
            return alternative1;
        }
        return alternative2;
    }

}

class CredentialSource extends Sources {
    Sink transformTo(Sink alternative1, Sink alternative2) {
        if (Susi.unlikely()) {
            return alternative1;
        }
        return alternative2;
    }
}


class Sink {
}

class EmailSink extends Sink {
}

class UsernameSink extends Sink {
}

class CredentialSink extends Sink {

}




package com.fourcasters.forec.reconciler.server.http;

import com.fourcasters.forec.reconciler.server.ReconcilerConfig;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.fourcasters.forec.reconciler.server.ProtocolConstants.RESPONSE_OK_HEADER;

/**
 * Created by ivan on 22/12/16.
 */
public class AlgoLogSenderServlet extends AbstractServlet {

    public AlgoLogSenderServlet(HttpParser httpParser) {
        super(httpParser);
    }

    @Override
    boolean validate(SocketChannel clientChannel) throws IOException {
        return validateMethod(httpParser, clientChannel, "GET", "Method must be GET")
                && validateParameter(httpParser, clientChannel, "magic", "Parameter 'magic' not well formatted");
    }

    @Override
    long respond(SocketChannel clientChannel) throws IOException {
        final String magicAsString = httpParser.getParam("magic");
        final String cross = httpParser.getParam("cross");
        final File logFile = findLogFile(magicAsString, cross);
        sendFile(clientChannel, RESPONSE_OK_HEADER, logFile.getAbsolutePath());
        return 0;
    }

    static String getAlgoFolderName(String magicAsString, String cross) {
        if (cross != null) {
            return magicAsString + "_" + cross;
        }
        else {
            return magicAsString;
        }
    }

    static String getLogFolderName(String magicAsString, String cross) {
        String folderName = "logfile_" + magicAsString;
        if (cross != null) {
            folderName += "_" + cross;
        }
        return folderName;
    }

    static String getLogFileName(String magicAsString, String cross) {
        String fileName = "logfile" + magicAsString;
        if (cross != null) {
            fileName += "_" + cross;
        }
        fileName += "_1.txt";
        return fileName;
    }

    static File findLogFile(String magicAsString, String cross) {
        final Path path = Paths.get(ReconcilerConfig.ALGO_PATH,
                getAlgoFolderName(magicAsString, cross),
                getLogFolderName(magicAsString, cross),
                getLogFileName(magicAsString, cross));
        return path.toFile();
    }
}

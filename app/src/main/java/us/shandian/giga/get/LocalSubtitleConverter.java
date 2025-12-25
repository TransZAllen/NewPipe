package us.shandian.giga.get;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import android.util.Log;

import org.schabi.newpipe.streams.io.SharpStream;

import org.schabi.newpipe.extractor.utils.SubtitleDeduplicator;

final class LocalSubtitleConverter {

    private static final String TAG = "LocalSubtitleConverter";

    private LocalSubtitleConverter() {
        // no instance
    }

    /**
     * Converts a local TTML subtitle file (file://) into VTT format
     * and stores it in the app's subtitle cache directory.
     *
     * @param localSubtitleUri file:// URI of the local TTML subtitle
     * @param mission current download mission
     * @return 0 if success, non-zero error code otherwise
     */
    public static int convertTtmlToVtt(String localSubtitleUri,
                                       DownloadMission mission) {

        if (!isValidLocalUri(localSubtitleUri)) {
            return 3;
        }

        File ttmlFile = new File(
                getAbsolutePathFromLocalUri(localSubtitleUri)
        );

        if (!ttmlFile.exists()) {
            mission.notifyError(DownloadMission.ERROR_FILE_CREATION, null);
            return 4;
        }

        if (!mission.storage.canWrite()) {
            mission.notifyError(DownloadMission.ERROR_PERMISSION_DENIED, null);
            return 5;
        }

        extractSubtitleFromTtmlToVtt(ttmlFile, mission);
        /*
        // 1. Read TTML
        String ttmlContent = readSubtitleFile(ttmlFile);
        if (ttmlContent == null) {
            return 5;
        }

        // 2. Deduplicate / normalize
        String vttContent = SubtitleDeduplicator
                .convertTtmlContentToVtt(ttmlContent);

        if (vttContent == null) {
            return 6;
        }

        // 3. Store VTT to cache
        File vttFile = storeVttToCache(vttContent, mission);

        if (vttFile == null) {
            return 7;
        }

        printLocalSubtitleConvertedOk(vttFile);
        */

        printLocalSubtitleConvertedOk(mission);

        return 0;
    }

    /* -------- private helpers -------- */

    private static boolean isValidLocalUri(String localUri) {
        String URL_PREFIX = SubtitleDeduplicator.LOCAL_SUBTITLE_URL_PREFIX;

        if (localUri.length() <= URL_PREFIX.length()) {
             return false;
        }

        return true;
    }

    private static String getAbsolutePathFromLocalUri(String localSubtitleUri) {
        String URL_PREFIX = SubtitleDeduplicator.LOCAL_SUBTITLE_URL_PREFIX;
        int prefixLength = URL_PREFIX.length();
        // Remove URL_PREFIX
        String absolutePath = localSubtitleUri.substring(prefixLength);
        return absolutePath;
    }

    // readSubtitleFile(...)
    // storeVttToCache(...)

    private static void extractSubtitleFromTtmlToVtt(File localFile,
                                              DownloadMission mission) {
        try (FileInputStream inputStream = new FileInputStream(localFile);
             SharpStream outputStream = mission.storage.getStream()) {

            byte[] buffer = new byte[DownloadMission.BUFFER_SIZE];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                mission.notifyProgress(bytesRead);
            }

            mission.length = totalBytes;
            mission.unknownLength = false;
            mission.notifyFinished();

        } catch (IOException e) {
            String logMessage = "Error extracting subtitle paragraphs from " +
                                    localFile.getAbsolutePath() + ", error:" +
                                    e.getMessage();
            Log.e(TAG, logMessage);
            mission.notifyError(DownloadMission.ERROR_FILE_CREATION, e);
        }
    }

    private static void printLocalSubtitleConvertedOk(DownloadMission mission) {
        try {
            String logMessage = "Local subtitle uri is extracted to:" +
                                mission.storage.getName();
            Log.i(TAG, logMessage);
        } catch (NullPointerException e) {
            Log.w(TAG, "Fail to convert ttml subtitle to vtt.", e);
        }
    }
}

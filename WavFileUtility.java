package sojournermobile.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by gpierce on 6/14/16.
 */
public class WavFileUtility
{
    private static final String LOG_TAG = "WavFileUtility";

    private static final int WAVE_MAGIC_NUMBER_HEADER_SIZE = 36;
    private static final int RECORDER_NUM_CHANNELS = 1;
    private static final int RECORDER_BITS_PER_SAMPLE = 16;
    private static final int RECORDER_SAMPLE_RATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private String filePath = Environment.getExternalStorageDirectory().getPath();


    private ByteArrayOutputStream recordedByteStream = new ByteArrayOutputStream();
    private AudioRecord aRecorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    public boolean isRecording()
    {
        return isRecording;
    }

    public void init( String outputFileName )
    {
        filePath.concat( outputFileName );

        bufferSize = AudioRecord.getMinBufferSize( RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING );

        if ( bufferSize == AudioRecord.ERROR_BAD_VALUE)
        {
            Log.e( LOG_TAG, "Unable to record audio with these settings");
        }
        else
        {
            Log.d(LOG_TAG, "*****************Min Buffer size " + bufferSize);

            aRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLE_RATE,
                    RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING,
                    bufferSize);

            if ( aRecorder.getState() == AudioRecord.STATE_INITIALIZED )
            {
                Log.d(LOG_TAG, "*****************Record State -- INITIALIZED ");


            }
            else
            {
                Log.d(LOG_TAG, ">>>>>>>>>>>>>>>>>  Record State -- ERROR. RECORDER NOT INITIALIZED PROPERLY!!!!");
            }

        }
    }


    public void startRecording()
    {
        // start the recording process
        //
        isRecording = true;

        recordingThread = new Thread( new Runnable()
        {
            public void run()
            {
                aRecorder.startRecording();
                writeAudioDataToBuffer();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

    }

    // recording has stopped, store the recorded bytes
    //
    public void stopRecording()
    {
        isRecording = false;
        aRecorder.stop();

        FileOutputStream fileOutputStream = null;
        try
        {
            fileOutputStream = new FileOutputStream(filePath);

            byte[] recordedBytes = recordedByteStream.toByteArray();

            // write out the header information
            //
            writeHeader( fileOutputStream, recordedBytes.length );

            // write out the PCM audio that was captured
            //
            writeAudio( fileOutputStream, recordedBytes );

            // close the file
            //
            fileOutputStream.close();

        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            // no mater what we need to release the recorder
            //
            aRecorder.release();
        }
    }

    private void writeHeader(OutputStream outputStream, int byteArraySize) throws IOException
    {

        long longSampleRate = RECORDER_SAMPLE_RATE;
        long byteRate = RECORDER_SAMPLE_RATE * RECORDER_NUM_CHANNELS * (RECORDER_BITS_PER_SAMPLE/8);
        long totalDataLen = WAVE_MAGIC_NUMBER_HEADER_SIZE + byteArraySize;


        byte[] header = new byte[44];
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' '; // 4 bytes: size of 'fmt ' chunk,
        header[16] = 16;  //  16 for PCM
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1 (PCM)
        header[21] = 0;
        header[22] = (byte) RECORDER_NUM_CHANNELS;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (RECORDER_NUM_CHANNELS * (RECORDER_BITS_PER_SAMPLE / 8));  // block align
        header[33] = 0;
        header[34] = RECORDER_BITS_PER_SAMPLE;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (byteArraySize & 0xff);
        header[41] = (byte) ((byteArraySize >> 8) & 0xff);
        header[42] = (byte) ((byteArraySize >> 16) & 0xff);
        header[43] = (byte) ((byteArraySize >> 24) & 0xff);

        outputStream.write(header, 0, 44);

        Log.d( LOG_TAG, "********************************************");
        Log.d( LOG_TAG, "Sample Rate: " + longSampleRate );
        Log.d( LOG_TAG, "Byte Rate:   " + byteRate );
        Log.d( LOG_TAG, "Block Align: " + (RECORDER_NUM_CHANNELS * (RECORDER_BITS_PER_SAMPLE / 8)));
        Log.d( LOG_TAG, "Bits Per Sample: " + RECORDER_BITS_PER_SAMPLE );
        Log.d( LOG_TAG, "Total Data Length: " + totalDataLen );
        Log.d( LOG_TAG, "Byte Array Size: " + byteArraySize );
        Log.d( LOG_TAG, "********************************************");

    }

    private void writeAudio(OutputStream outputStream, byte[] recordedBytes ) throws IOException{
        Log.d( LOG_TAG, "Storing [" + recordedBytes.length + "] bytes into the file");
        outputStream.write( recordedBytes );
    }

    private void writeAudioDataToBuffer()
    {
        // Write the output audio in byte
        short sData[] = new short[bufferSize/2];

        while (isRecording)
        {
            // gets the voice output from microphone to byte format

            aRecorder.read(sData, 0, bufferSize/2);

            try
            {
                // // stores the voice buffer
                byte bData[] = short2byte(sData);

                recordedByteStream.write( bData );

                Log.d( LOG_TAG, "There are " + recordedByteStream.size() + " PCM data bytes stored");

                //os.write(bData, 0, bufferSize);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }
}


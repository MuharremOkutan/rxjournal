package org.rxjournal.examples.helloworld;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import org.junit.Assert;
import org.junit.Test;
import org.rxjournal.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  A demo example Junit test class to test BytesToWordsProcessor.
 *  Make sure you have run the HelloWorldApp_JounalAsObserver first to generate the journal.
 */
public class HelloWorldTest {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldTest.class.getName());

    @Test
    public void testHelloWorld() throws IOException, InterruptedException {
        //Create the rxRecorder but don't delete the cache that has been created.
        RxJournal rxJournal = new RxJournal(HelloWorldApp_JounalAsObserver.FILE_NAME);

        //Get the input from the recorder
        RxPlayer rxPlayer = rxJournal.createRxPlayer();
        //In this case we can play the data stream in FAST mode.
        PlayOptions options= new PlayOptions().filter(HelloWorldApp_JounalAsObserver.INPUT_FILTER)
                .replayRate(PlayOptions.ReplayRate.FAST);
        //Use a ConnectableObservable as we only want to kick off the stream when all
        //connections have been wired together.
        ConnectableObservable<Byte> observableInput = rxPlayer.play(options).publish();

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        Observable<String> observableOutput = bytesToWords.process(observableInput);

        //Send the output stream to the recorder to be validated against the recorded output
        RxValidator rxValidator = rxJournal.createRxValidator();
        Observable<ValidationResult> results = rxValidator.validate(HelloWorldApp_JounalAsObserver.FILE_NAME,
                observableOutput, HelloWorldApp_JounalAsObserver.OUTPUT_FILTER);

        CountDownLatch latch = new CountDownLatch(1);
        results.subscribe(
                s->LOG.info(s.toString()),
                e-> LOG.error("Problem in process test [{}]", e),
                ()->{
                    LOG.info("Summary[" + rxValidator.getValidationResult().summaryResult()
                            + "] items compared[" + rxValidator.getValidationResult().summaryItemsCompared()
                            + "] items valid[" + rxValidator.getValidationResult().summaryItemsValid() +"]");
                    latch.countDown();
                });

        observableInput.connect();
        boolean completedWithoutTimeout = latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(ValidationResult.Result.OK, rxValidator.getValidationResult().getResult());
        Assert.assertTrue(completedWithoutTimeout);
    }
}

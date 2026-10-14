package com.fiteasy.desktop

import com.fiteasy.shared.ActiveSecondsClock
import kotlin.test.*

class ActiveSecondsClockTest {
    @Test fun delayedUiCountsActualSeconds(){val c=ActiveSecondsClock();c.resume(0);assertEquals(5,c.sample(5_400_000_000));assertEquals(1,c.sample(6_100_000_000))}
    @Test fun pausedTimeExcludedAndRemainderSurvives(){val c=ActiveSecondsClock();c.resume(0);assertEquals(0,c.sample(700_000_000));c.pause();assertEquals(0,c.sample(10_000_000_000));c.resume(20_000_000_000);assertEquals(1,c.sample(20_300_000_000))}
    @Test fun repeatedResumeDoesNotResetAndBackwardClockIgnored(){val c=ActiveSecondsClock();c.resume(1_000_000_000);c.resume(1_800_000_000);assertEquals(0,c.sample(500_000_000));assertEquals(1,c.sample(2_000_000_000))}
    @Test fun extremeDelayBounded(){val c=ActiveSecondsClock();c.resume(0);assertEquals(86400,c.sample(100_000_000_000_000))}
    @Test fun pauseKeepsTimeBetweenLastSampleAndStageChange(){val c=ActiveSecondsClock();c.resume(0);assertEquals(0,c.sample(700_000_000));c.pause(900_000_000);c.resume(5_000_000_000);assertEquals(1,c.sample(5_100_000_000))}
}

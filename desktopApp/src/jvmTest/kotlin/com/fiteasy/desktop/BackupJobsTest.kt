package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlin.test.*
import kotlinx.serialization.encodeToString

class BackupJobsTest {
    @Test fun plainAndEncryptedBackupsPreserveDataAndOptionalAppearance(){
        val data=SharedData(entries=listOf(Entry(type="drink",name="Water",amount=250.0,unit="ml",plainWater=true)))
        val appearance=Appearance(language="ja")
        listOf("","testing-password").forEach {password->val result=readBackup(prepareBackup(data,emptyList(),appearance,password),password);assertEquals(data,result.data);assertEquals(appearance,result.appearance)}
        assertNull(readBackup(prepareBackup(data,emptyList(),null,""),"").appearance)
    }
    @Test fun invalidAppearanceAndUnknownFormatRejectedBeforeApply(){
        assertFails {readBackup(codec.encodeToString(Backup(data=SharedData(),appearance=Appearance(language="invalid"))).toByteArray(),"")}
        assertFails {readBackup(codec.encodeToString(Backup(format="invalid",data=SharedData())).toByteArray(),"")}
        assertFails {readBackup(prepareBackup(SharedData(),emptyList(),null,"testing-password"),"wrong-password")}
    }
}

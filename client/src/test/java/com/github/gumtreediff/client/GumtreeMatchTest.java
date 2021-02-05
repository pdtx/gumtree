package com.github.gumtreediff.client;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GumtreeMatchTest {

    @Test
    void matchStatementUpdate() {
        String src = "D:\\gumtree\\javaDiff\\";
        String commitId = "0f0c54b085eb81c793c1fd129e3e87d8434cb40d" +"\\";
        String srcFile = src + commitId + "src_main_java_com_test_packageTest3_test13.java";
        String dstFile = src + commitId + "src_main_java_com_test_packageTest3_test13-dst.java";

        Map<String, Set<String>> result = GumtreeMatch.matchFile(srcFile, dstFile);
        java.util.List<String> expectedInsert = Arrays.asList();
        java.util.List<String> expectedDelete = Arrays.asList();
        java.util.List<String> expectedMove= Arrays.asList();
        java.util.List<String> expectedUpdate = Arrays.asList(
                "87-87#87-87",
                "88-88#88-88",
                "89-89#89-89",
                "11-11#11-11"
        );

        verify(result, expectedInsert, expectedDelete, expectedUpdate, expectedMove);
    }


    @Test
    void matchChildrenStatementUpdate() {
        String src = "D:\\gumtree\\javaDiff\\";
        String commitId = "0a21bdf86454ddc561e25c966f48156bb347eab9" +"\\";
        String srcFile = src + commitId + "src_main_java_com_test_packageTest3_test13.java";
        String dstFile = src + commitId + "src_main_java_com_test_packageTest3_test13-dst.java";

        Map<String, Set<String>> result = GumtreeMatch.matchFile(srcFile, dstFile);
        java.util.List<String> expectedInsert = Arrays.asList();
        java.util.List<String> expectedDelete = Arrays.asList();
        java.util.List<String> expectedMove= Arrays.asList();
        java.util.List<String> expectedUpdate = Arrays.asList(
                "80-80#80-80",
                "82-82#82-82",
                "87-87#87-87",
                "88-88#88-88",
                "111-111#111-111"
        );

        verify(result, expectedInsert, expectedDelete, expectedUpdate, expectedMove);
    }

    private void verify(Map<String, Set<String>> result, List<String> expectedInsert, List<String> expectedDelete, List<String> expectedUpdate, List<String> expectedMove) {
        // 缺失匹配
        for(String s : expectedInsert){
            assertTrue(result.get(GumtreeMatch.KEY_INSERT).contains(s),
                    "lost insert: "+ s);
        }
        for(String s : expectedDelete){
            assertTrue(result.get(GumtreeMatch.KEY_DELETE).contains(s),
                    "lost delete: "+ s);
        }
        for(String s : expectedUpdate){
            assertTrue(result.get(GumtreeMatch.KEY_UPDATE).contains(s),
                    "lost update: "+ s);
        }
        for(String s : expectedMove){
            assertTrue(result.get(GumtreeMatch.KEY_MOVE).contains(s),
                    "lost move: "+ s);
        }

        // 多余匹配
        for(String s : result.get(GumtreeMatch.KEY_INSERT)){
            assertTrue(expectedInsert.contains(s),
                    "redundant insert: "+ s);
        }
        for(String s : result.get(GumtreeMatch.KEY_DELETE)){
            assertTrue(expectedDelete.contains(s),
                    "redundant delete: "+ s);
        }
        for(String s : result.get(GumtreeMatch.KEY_UPDATE)){
            assertTrue(expectedUpdate.contains(s),
                    "redundant update: "+ s);
        }
        for(String s : result.get(GumtreeMatch.KEY_MOVE)){
            assertTrue(expectedMove.contains(s),
                    "redundant move: "+ s);
        }
    }

}
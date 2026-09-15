package com.moli.scene.learn;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = SceneLearnApplication.class)
@Rollback
@Transactional(rollbackFor = Exception.class)
public class BaseTest {

}

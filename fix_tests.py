with open('/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-communication/src/test/java/com/nadoumi/communication/service/MessagePublisherTest.java', 'r') as f:
    content = f.read()

# I will just write a new setUpMockId body
to_replace = """    @org.junit.jupiter.api.BeforeEach
    void setUpMockId() {

    }"""

replacement = """    @org.junit.jupiter.api.BeforeEach
    void setUpMockId() {
        org.mockito.Mockito.doAnswer(inv -> {
            inv.<com.nadoumi.communication.domain.Message>getArgument(0).setId(50L);
            return 1;
        }).when(messages).insert(org.mockito.ArgumentMatchers.any());
    }"""

content = content.replace(to_replace, replacement)

with open('/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-communication/src/test/java/com/nadoumi/communication/service/MessagePublisherTest.java', 'w') as f:
    f.write(content)

The example in this folder has the following dependencies:

1. EkstaziE -> [EkstaziE]
2. EkstaziD -> [EkstaziD]
3. EkstaziC -> [EkstaziC]
4. EkstaziB -> [EkstaziB]
5. EkstaziA -> [EkstaziA, EkstaziB]
6. EkstaziDTest -> [EkstaziDTest, EkstaziD]
7. EkstaziCTest -> [EkstaziCTest, EkstaziC]
8. EkstaziBTest -> [EkstaziBTest, EkstaziB]
9. EkstaziATest -> [EkstaziATest, EkstaziA, EkstaziB]

Changing EkstaziE should force the execution of no test.

Changing EkstaziD should force the execution of all tests in EkstaziDTest.

Changing EkstaziC should force the execution of all tests in EkstaziCTest.

Changing EkstaziB should force the execution of all tests in EkstaziATest and EkstaziBTest.

Changing EkstaziA should force the execution of all tests in EkstaziATest.
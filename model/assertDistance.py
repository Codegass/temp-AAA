# Calculate the Assert Distance based on one of the two methods
import numpy as np

class AssertDistance():

    def __init__(self, method:str):
        '''
        INPUT: 
        lineNumebr: line number list, each of the item is like this [123-125]
        AAA: AAA tag list, each of the item is one of the 0,1,2.

        OUTPUT:
        assertDistanceList: the list of the each statement's distance to Assert.

        The user can choose line number or AAA tag to calculate the distance.
        '''
        self.method = method

        #Check the method is either "lineNumber" or "AAA"
        if self.method not in ["lineNumber", "AAA"]:
            raise ValueError("The method should be either lineNumber or AAA.")
        

    def getAssertDistanceList(self, lineNumberList:list, AAAList:list, levelList:list=None):
        '''Result Getter'''
        if self.method == "lineNumber":
            self.__assertDistanceList = self.assertDistanceByLineNumber(lineNumberList, AAAList, levelList)

        if self.method == "AAA":
            self.__assertDistanceList = self.assertDistanceByAAA(AAAList)
        return self.__assertDistanceList
    
    def assertDistanceByLineNumber(self, lineNumberList:list, AAAList:list, levelList:list):
        '''
        Two types of line number in the list: [123-125] or [123-123]. Also we need to merge the multiple line code into single line.
        This Function does in the following steps:
        1. Find all the assertion line (by checking the AAAList with the value 2, lineNumberList and AAAList share the same index) and merge all the line in the assertion lines interval into one line, which means their Assert
        distance is 0.
        2. Merge the rest of the lines based on the interval, if they are in the same interval, then merge them into one line, which means
        their Assert distance is the same.
        3. Calculate the Assert distance based on the merged line number list.

        For example, if the lineNumberList is [[123-123],[123-125],[126-128],[127-128],[129-129],[130-132],[131-132]], and based on the AAAList,
        the assertion line is the [130-132]. Then the result should be [3,3,2,2,1,0,0]. Becasaue [123-125],[123-123] are merged into same line, 
        [126-128],[127-128] are merged into same line, [129-129] is a single line, [130-132],[131-132] are merged into same line.

        #TODO: Special Handling now, if the function is expanded, then the expanded function will share the same assert distance with the parent function.
        This is because the idea of the merge line is to identify the true in mind order of the function when developers write the code.
        '''
        # Step 0: Check the input list with levelList
        parent = lineNumberList[0]
        for i in range(len(lineNumberList)):
            if levelList[i] != 0:
                lineNumberList[i] = parent
            else:
                parent = lineNumberList[i]
                
        # Step 1: Merge line numbers into unique intervals
        mergedLines = []
        for lineNumber in lineNumberList:
            start, end = map(int, lineNumber.strip("[]").split("-"))
            if not mergedLines or start > mergedLines[-1][1]:
                mergedLines.append([start, end])
            else:
                mergedLines[-1][1] = max(end, mergedLines[-1][1])
                mergedLines[-1][0] = min(start, mergedLines[-1][0])

        # Step 2: Calculate assert distances
        assertDistances = [-2] * len(lineNumberList)
        intervalDistanceList = [-2] * len(mergedLines)

        # Highlight the intvals that contain assertion lines
        for i, (start, end) in enumerate(mergedLines):
            for j, lineNumber in enumerate(lineNumberList):
                lineStart, lineEnd = map(int, lineNumber.strip("[]").split("-"))
                if lineStart >= start and lineEnd <= end and AAAList[j] == 2:
                    assertDistances[j] = 0
                    intervalDistanceList[i] = 0

        # calculate the distance from the interval to the assertion interval
        for i in range(len(mergedLines) - 1, -1, -1):
            if intervalDistanceList[i] == -2:
                intervalDistanceList[i] = -1 if i == len(mergedLines) - 1 else intervalDistanceList[i + 1] + 1

        # Step 3: Update assert distances based on interval distances
        for j, lineNumber in enumerate(lineNumberList):
            lineStart, lineEnd = map(int, lineNumber.strip("[]").split("-"))
            for i, (start, end) in enumerate(mergedLines):
                if lineStart >= start and lineEnd <= end:
                    if assertDistances[j] == -2:
                        assertDistances[j] = intervalDistanceList[i]

        return [np.int64(d) for d in assertDistances]

    def assertDistanceByAAA(self, AAAList:list):
        '''
        Calculate the Assert Distance based on each of statement's distance to the nearest Assert statement which is 2 in
        the AAAList. If the statement does not have any following Assert statement, then the distance is -1. And for the Assert
        statement, the distance is 0. For example, if the AAAList is [0,1,1,2,0,1,0,2,0,0], then the assertDistanceList is 
        [3,2,1,0,3,2,1,0,-1,-1].
        '''
        distanceList = []
        distance = -1

        # Loop through the AAAList in reverse
        for aaa in reversed(AAAList):
            if aaa == 2:
                distance = 0
            else:
                if distance != -1:
                    distance += 1
            distanceList.insert(0, np.int64(distance))

        return distanceList
    

if __name__ == "__main__":
    # Test the assertDistanceByLineNumber
    lineNumberList = ["[123-123]","[123-125]","[126-128]","[127-128]","[129-129]","[130-132]","[131-132]","[133-133]"]
    AAAList = [0,0,0,0,0,2,0,0]
    levelList = [0,0,0,0,0,0,0,0]
    assertDistanceObj = AssertDistance("lineNumber")
    assertDistanceList = assertDistanceObj.getAssertDistanceList(lineNumberList, AAAList, levelList)
    assert assertDistanceList == [3, 3, 2, 2, 1, 0, 0, -1]

    # Test case 2
    lineNumberList = ['[33-33]','[34-34]','[34-34]','[35-35]','[35-35]','[35-35]','[36-36]','[38-38]','[38-38]','[39-39]','[39-39]','[39-39]','[40-40]','[40-40]','[40-40]']
    AAAList = [0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 2, 0, 2, 0]
    levelList = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]
    assertDistanceList = assertDistanceObj.getAssertDistanceList(lineNumberList, AAAList, levelList)
    assert assertDistanceList == [4, 3, 3, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0]

    # Test Case 3 there are expanded line number
    lineNumberList = ["[12-14]", "[134-135]","[135-135]", "[14-14]"] #TODO: one line case is hard to handle
    AAAList = [0, 0, 0, 2]
    levelList = [0,1,1,0]
    assertDistanceList = assertDistanceObj.getAssertDistanceList(lineNumberList, AAAList, levelList)
    print(assertDistanceList)

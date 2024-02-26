# Get the statment type from erach of the method name columen and build them into matrixs or one hot encoding.


class StatementType:
    '''
    The class process potentialTargetQualifiedName and based on the keyword
    parse the statement type into ASSERT, MOCK, THIRD, GET, SET, NEW.
    '''

    def __init__(self) -> None:
        #TODO: different encoding method will be added in thte future
        pass

    def parseStatementType(self, potentialTargetQualifiedNameList:list) -> dict:
        '''
        Parse the statement type based on the potentialTargetQualifiedNameList.
        '''
        # based on the input list's len, init the dict, each of the item is a list full of 0
        statementTypeDict = {"ASSERT":[], "MOCK":[], "THIRD":[], "GET":[], "SET":[], "NEW":[]}
        
        for potentialTargetQualifiedName in potentialTargetQualifiedNameList:
            if potentialTargetQualifiedName is None:
                # if the potentialTargetQualifiedName is None, then all the statement type should be 0
                statementTypeDict["ASSERT"].append(0)
                statementTypeDict["MOCK"].append(0)
                statementTypeDict["THIRD"].append(0)
                statementTypeDict["GET"].append(0)
                statementTypeDict["SET"].append(0)
                statementTypeDict["NEW"].append(0)
                continue
            # if the potentialTargetQualifiedName is not None, then we need to parse the statement type based on the keyword
            if "ASSERT" in potentialTargetQualifiedName:
                statementTypeDict["ASSERT"].append(1)
            else:
                statementTypeDict["ASSERT"].append(0)
            
            if "MOCK" in potentialTargetQualifiedName:
                statementTypeDict["MOCK"].append(1)
            else:
                statementTypeDict["MOCK"].append(0)
            
            if "THIRD" in potentialTargetQualifiedName:
                statementTypeDict["THIRD"].append(1)
            else:
                statementTypeDict["THIRD"].append(0)
            
            if "GET" in potentialTargetQualifiedName:
                statementTypeDict["GET"].append(1)
            else:
                statementTypeDict["GET"].append(0)
            
            if "SET" in potentialTargetQualifiedName:
                statementTypeDict["SET"].append(1)
            else:
                statementTypeDict["SET"].append(0)
            
            if "NEW" in potentialTargetQualifiedName:
                statementTypeDict["NEW"].append(1)
            else:
                statementTypeDict["NEW"].append(0)

        return statementTypeDict

    def getStatementTypeDict(self,potentialTargetQualifiedNameList:list) -> dict:
        '''
        Getter for the statementTypeDict
        '''
        return self.parseStatementType(potentialTargetQualifiedNameList)
    

if __name__ == "__main__":
    # test the statementType class
    testList = ["ASSERT dfdfe()", "   MOCK ehdufedassert()", "THIRD GET ffuend()", "GET set()", "SET dfeu ", "     NEW", None]
    st = StatementType()
    print(st.getStatementTypeDict(testList))
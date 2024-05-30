import os
import logging
import pandas as pd
from nameSimilarity import NameSimilarity
from assertDistance import AssertDistance
from statementType import StatementType

 
class FeatureBuilding():
    '''
    Build the features for xgboost model.
    The Level Feature indicates the function expansion level, for any expanded function, the level will be parent level+1
    '''

    def __init__(self, projectRoot, model="CB", assert_distance_approach="lineNumber", statement_type_approach=None) -> None:
        # set the logger save the log file to the current folder
        logging.basicConfig(filename='featureBuilding.log', filemode='w', format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', level=logging.INFO)
        self.logger = logging.getLogger(__name__)
        self.projectRoot = projectRoot
        self.csv_files = self.find_all_csv_files_in_parsed_folder(projectRoot, target_folder_name="AAA")

        # set the approach for the each feature's building
        self.ns = NameSimilarity(word2vec_model=model)
        self.ad = AssertDistance(method=assert_distance_approach)
        self.st = StatementType()
        
    def find_all_csv_files_in_parsed_folder(self, projectRoot, target_folder_name="AAA") -> list:
        '''
        Find all csv files under the root directory's target folder.
        '''
        csv_files = []
        for root, dirs, files in os.walk(projectRoot):
            if root.endswith(os.path.join(target_folder_name,"parsed")):
                for file in files:
                    if file.endswith(".csv"):
                        csv_files.append(os.path.join(root, file))
            
        return csv_files
    
    def buildFeatures(self):

        # create the feature folder under project root's subfolder AAA
        if not os.path.exists(os.path.join(self.projectRoot, "AAA", "feature")):
            os.makedirs(os.path.join(self.projectRoot, "AAA", "feature"))
            

        for csv in self.csv_files:
            print(f"build the feature for {csv}")
            result_df = pd.DataFrame(columns=["testPackage", "testClassName", "testMethodName", "potentialTargetQualifiedName", "AAA", "lineNumber", "name_similarity", "assert_distance", "level"])
            self.logger.info(f"build the feature for {csv}")
            df = pd.read_csv(csv) # columns ["testPackage", "testClassName", "testMethodName", "potentialTargetQualifiedName", "AAA(0,1,2)", "lineNumber"] are useful
            
            #handle the empty df
            if df.empty:
                self.logger.info(f"the {csv} is empty")
                continue

            # build the level feature
            result_df['level'] = df['potentialTargetQualifiedName'].apply(self.calculate_level)
            
            # handle the df with only one row
            if len(df) == 1:
                self.logger.info(f"the {csv} has only one row")
                result_df["testPackage"] = df["testPackage"]
                result_df["testClassName"] = df["testClassName"]
                result_df["testMethodName"] = df["testMethodName"]
                result_df["potentialTargetQualifiedName"] = df["potentialTargetQualifiedName"]
                result_df["lineNumber"] = df["lineNumber"]
                result_df["AAA"] = df["AAA(0,1,2)"]
                result_df["name_similarity"]= 0
                result_df["assert_distance"] = 0
                # result_df["level"] = 0
                result_df = pd.concat([result_df, pd.DataFrame(self.st.getStatementTypeDict(df["potentialTargetQualifiedName"].tolist()))], axis=1)
                result_df.to_csv(csv.replace("parsed", "feature"), index=False, quoting=pd.CSV_QUOTE_NONNUMERIC)
                continue

            # handle the df AAA(0,1,2) column are all 2 or 0
            if df["AAA(0,1,2)"].tolist().count(2) == len(df["AAA(0,1,2)"].tolist()) or df["AAA(0,1,2)"].tolist().count(0) == len(df["AAA(0,1,2)"].tolist()):
                self.logger.info(f"the {csv} has all the AAA(0,1,2) are 2 or 0")
                result_df["testPackage"] = df["testPackage"]
                result_df["testClassName"] = df["testClassName"]
                result_df["testMethodName"] = df["testMethodName"]
                result_df["potentialTargetQualifiedName"] = df["potentialTargetQualifiedName"]
                result_df["lineNumber"] = df["lineNumber"]
                result_df["AAA"] = df["AAA(0,1,2)"]
                result_df["name_similarity"]= 0
                result_df["assert_distance"] = 0
                result_df = pd.concat([result_df, pd.DataFrame(self.st.getStatementTypeDict(df["potentialTargetQualifiedName"].tolist()))], axis=1)
                result_df.to_csv(csv.replace("parsed", "feature"), index=False, quoting=pd.CSV_QUOTE_NONNUMERIC)
                continue

            result_df["testPackage"] = df["testPackage"]
            result_df["testClassName"] = df["testClassName"]
            result_df["testMethodName"] = df["testMethodName"]
            result_df["potentialTargetQualifiedName"] = df["potentialTargetQualifiedName"]
            result_df["lineNumber"] = df["lineNumber"]
            result_df["AAA"] = df["AAA(0,1,2)"]

            result_df["name_similarity"]= self.ns.genNameSimilarity(df["potentialTargetQualifiedName"].tolist(), df["testMethodName"].tolist())
            result_df["assert_distance"] = self.ad.getAssertDistanceList(df["lineNumber"].tolist(), df["AAA(0,1,2)"].tolist(), result_df["level"].tolist())

            result_df = pd.concat([result_df, pd.DataFrame(self.st.getStatementTypeDict(df["potentialTargetQualifiedName"].tolist()))], axis=1)
            
            # after checking the result
            result_df = self.after_checking(df, result_df, self.projectRoot)
            
            result_df.to_csv(csv.replace("parsed", "feature"), index=False, quoting=pd.CSV_QUOTE_NONNUMERIC)

    def calculate_level(self, string):
        '''
        Calculate the level feature based on the potentialTargetQualifiedName.
        if the potentialTargetQualifiedName has 5*n space in the beginning, then the level is n.
        each row potentialTargetQualifiedName column is a string like "     org.apache.commons.cli.Parser.parse(Options, String[], Properties)"
        '''
        space_count = 0
        for char in string:
            if char == ' ':
                space_count += 1
            else:
                break
        return space_count // 5
    
    def after_checking(self, input_df: pd.DataFrame, result_df: pd.DataFrame, projectRoot: str) -> pd.DataFrame:
        '''
        Ensure the lengths of the two DataFrames are consistent.
        '''
        # If the number of rows is the same, return result_df directly
        if len(input_df) == len(result_df):
            return result_df
        
        # First, reset the index of both dfs to ensure continuity, facilitating subsequent operations
        input_df = input_df.reset_index(drop=True)
        result_df = result_df.reset_index(drop=True)

        # Rename the AAA(0,1,2) column in input_df to AAA for consistency with result_df
        input_df = input_df.rename(columns={'AAA(0,1,2)': 'AAA'})

        i = 0  # Set initial index
        while i < len(input_df) and len(result_df) < len(input_df):
            # Compare the value of potentialTargetQualifiedName at the same position in both DataFrames
            if i >= len(result_df) or input_df.at[i, 'potentialTargetQualifiedName'] != result_df.at[i, 'potentialTargetQualifiedName']:
                # If they do not match, copy the missing row from input_df to the corresponding position in result_df
                # Including specific columns
                row_to_insert = {
                    'testPackage': input_df.at[i, 'testPackage'],
                    'testClassName': input_df.at[i, 'testClassName'],
                    'testMethodName': input_df.at[i, 'testMethodName'],
                    'potentialTargetQualifiedName': input_df.at[i, 'potentialTargetQualifiedName'], 
                    'AAA': input_df.at[i, 'AAA'], 
                    'lineNumber': input_df.at[i, 'lineNumber']
                }
                # Create a DataFrame with only the target columns
                row_df = pd.DataFrame([row_to_insert], index=[i])
                result_df = pd.concat([result_df.iloc[:i], row_df, result_df.iloc[i:]]).reset_index(drop=True)
            i += 1  # Move to the next index if matched or after insertion

        try:
            with open(os.path.join(projectRoot,"AAA","after_checking.txt"), "a") as f:
                # record the testclass name and test method name
                f.write("feature building\n")
                f.write(f"{result_df.at[0, 'testClassName']} {result_df.at[0, 'testMethodName']}\n")
        except:
            pass

        return result_df


if __name__ == "__main__":
    # Calculate the excution time
    import time
    start = time.time()

    # build the feature
    fb = FeatureBuilding(projectRoot="/Users/chenhaowei/Documents/github/commons-cli/")
    fb.buildFeatures()

    end = time.time()

    print(f"Total time cost is {end-start} seconds")
from featureBuilding import FeatureBuilding
import pandas as pd
import xgboost as xgb
import argparse
import os

def main():
    parser = argparse.ArgumentParser(description="Feature Building and Prediction")
    parser.add_argument("projectRoot", type=str, help="The root path of the project")
    parser.add_argument("modelPath", type=str, help="The path of the xgboost model")
    args = parser.parse_args()
    fb = FeatureBuilding(args.projectRoot)
    fb.buildFeatures() # feature building file will be saved in the projectRoot/AAA/feature folder
    model_runner(args.projectRoot, args.modelPath) # predict the result based on the xgboost model

def after_checking(input_df: pd.DataFrame, result_df: pd.DataFrame, projectRoot: str) -> pd.DataFrame:
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
                f.write("Classification\n")
                f.write(f"{result_df.at[0, 'testClassName']} {result_df.at[0, 'testMethodName']}\n")
        except:
            pass
        return result_df

def model_runner(projectRoot:str, model_file_path:str):
    ''' Running the xgboost based on the xgboost.model file'''
    model = xgb.XGBClassifier()
    model.load_model(model_file_path)

    # find all the csv files under the projectRoot/AAA/feature folder
    csv_files = []
    for root, dirs, files in os.walk(os.path.join(projectRoot, "AAA", "feature")):
        for file in files:
            if file.endswith(".csv"):
                csv_files.append(os.path.join(root, file))
    
    # predict the result for each csv file
    for csv_file_path in csv_files:
        print(f"predict the result for {csv_file_path}")
        data = pd.read_csv(csv_file_path)

        # handle the empty df
        if data.empty:
            continue
        
        # predict the result
        # Select relevant columns (ensure these columns exist in all your CSV files)
        X = data[['name_similarity', 'assert_distance', 'level', 'ASSERT', 'MOCK', 'THIRD', 'GET', 'SET', 'NEW']]

        # Predict
        y_pred = model.predict(X)
        
        # Merge the y and y_pred, if any of the value is 2, then the result should be 2
        y = data['AAA'].values
        result = [2 if y_val == 2 or pred_val == 2 else int(pred_val) for y_val, pred_val in zip(y, y_pred)]
        data['AAA'] = result

        # after checking the result
        data = after_checking(pd.read_csv(csv_file_path.replace("feature","parsed")), data, projectRoot)

        # Save the result to the csv file
        data.to_csv(csv_file_path, index=False)

if __name__ == "__main__":
    main()
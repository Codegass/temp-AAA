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

        # Save the result to the csv file
        data.to_csv(csv_file_path, index=False)

if __name__ == "__main__":
    main()
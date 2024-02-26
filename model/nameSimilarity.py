# Call the CodeBERT API or using the local W2V model to calculate the similarity between two names
import logging
import distance
import requests
import tqdm, os  # noqa: E401
import numpy as np
import sklearn as skl
from gensim.models.keyedvectors import KeyedVectors
from wordCleaning import WordCleaning

class NameSimilarity:
    '''
    All the methods for the calculation of the Name similarity between class&Case name and method name.
    the input should be the 
    '''
    def __init__(self,word2vec_model:str='CB'):
        logging.info(f"Current Running Path :{os.path.dirname(os.path.abspath(__file__))}")
        self.word2vec_model = word2vec_model
        if word2vec_model == 'SO': # Stack Over Flow Word2Vec the vector size is 200
            # try to find the model in the local
            if not os.path.exists('SO_vectors_200.bin'):
                # if the model is not exist, then dowlnoad it from the internet
                self.download_SO_word2vec()
            self.word_vec = KeyedVectors.load_word2vec_format('SO_vectors_200.bin', binary=True)
        elif word2vec_model == 'CB': # CodeBERT Embedding the vesctor size is 768
            #TODO: API INIT with the Internet checking
            self.embedding = word2vec_model
    
    def list_to_str(self,input_list):
        merged_str = ''
        for i in range(len(input_list)):
            if i != len(input_list):
                merged_str = merged_str + input_list[i] + ' '
            else:
                merged_str = merged_str + input_list[i]
        return merged_str
    
    def genNameSimilarity(self, testMethodNameList:list,caseNameList:list):
        '''
        Generate the name similarity based on the input list.
        testMethodNameList: the list of the test method name, like ["NEW org.apache.commons.cli.CommandLine.Builder()","...",...]
        caseNameList: the list of the case name, like ["testBuilder","...",...]
        '''
        nameSimilarityList = []
        wc = WordCleaning()
        # print(f"testMethodNameList is {testMethodNameList}")
        # print(f"caseNameList is {caseNameList}")

        if self.word2vec_model == "SO":
            for testMethodName, caseName in zip(testMethodNameList, caseNameList):
                self.testMethodName = wc.cleanWord(testMethodName)
                # print(f"the test method name is {self.testMethodName}")
                self.caseName = wc.cleanWord(caseName)
                # print(f"the case name is {self.caseName}")
                # print("------")

                nameSimilarityList.append(self.Word2Vec_SO())
        elif self.word2vec_model == "CB":
            # The logic for the CodeBERT API should be different
            # We need to consider of the calling rate
            cleanMethodList = []
            cleanMethodList.append(wc.cleanWord(caseNameList[0])) # the first word is the case name
            for testMethodName in testMethodNameList:
                self.testMethodName = wc.cleanWord(testMethodName)
                cleanMethodList.append(self.testMethodName) # the rest of the words are the test method name  
            # print(f"the cleanMethodList is {cleanMethodList}")
            nameSimilarityList = self.codebertAPI(cleanMethodList)
        
        return nameSimilarityList


    def editDistance(self):
        '''Name Similarity based on the edit distance'''
        return distance.levenshtein(self.testMethodName,self.caseName)

    def jacard_similarity(self):
        '''Name Similarity based on the jacard similarity'''
        s1= self.list_to_str(self.testMethodName)
        s2= self.list_to_str(self.caseName)
        # convert to TF Matrix
        cv = skl.feature_extraction.text.CountVectorizer(tokenizer=lambda s: s.split())
        corpus = [s1,s2]
        vectors = cv.fit_transform(corpus).toarray()
        #intersection
        numerator = np.sum(np.min(vectors,axis=0))
        #Union
        denominator = np.sum(np.max(vectors,axis=0))
        #Jacard
        return 1.0 * numerator/denominator
    
    def cosine_similarity(self,x,y):
        '''Name Similarity based on the cosine similarity'''
        # if x and y is not np.array, then convert them into np.array
        if not isinstance(x, np.ndarray):
            x = np.array(x)
        if not isinstance(y, np.ndarray):
            y = np.array(y)
        num = x.dot(y.T)
        denom = np.linalg.norm(x) * np.linalg.norm(y)
        return num / denom

    def Word2Vec_SO(self) -> float: 
        '''
        Use the Stack over Flow dic for Word2Vec.
        Here we use the simpliest way average to calculate the sentence vector
        '''
        casenameVec = np.zeros(200)
        testMethodNameVec = np.zeros(200)
        case_count = 0
        for word in self.caseName:
            case_count += 1
            try:
                vec = self.word_vec.get_vector(word)
            except Exception as e:
                vec = np.zeros(200)
                logging.warning(e)
            casenameVec = casenameVec+vec

        AVG_case_Vec = casenameVec/case_count

        method_count = 0
        for word in self.testMethodName:
            method_count += 1
            try:
                vec = self.word_vec.get_vector(word)
            except Exception as e:
                vec = np.zeros(200)
                logging.warning(e)
            testMethodNameVec = testMethodNameVec +vec
        AVG_testMethodNameVec = testMethodNameVec/method_count

        result_similarity = self.cosine_similarity(AVG_case_Vec,AVG_testMethodNameVec)
        if not result_similarity:
            print('case name is {}'.format(self.caseName))
            print("AVG_case_VEC:")
            print(AVG_case_Vec)
            print("AVG_testMethodNameVec:")
            print(AVG_testMethodNameVec)
        return result_similarity

    def codebertAPI(self,case_and_method_name_list) -> list:
        '''
        Use the CodeBERT API to calculate the similarity between two case name and method names
        TODO: There are two options for the embedding
        1. build all the parsed words into one sentence and send to the API and build as the sentence embedding
        get the first embedding.
        2. build embedding for each of the words and build build as the avg embedding

        Here we use the first option as Beginning
        '''
        url = 'http://localhost:8000/generate-embedding' #TODO: NEED TO CHANGE TO THE REAL API
        headers = {'accept': 'application/json', 'Content-Type': 'application/json'}
        
        # pre process the input list
        for i in range(len(case_and_method_name_list)):
            case_and_method_name_list[i] = " ".join(case_and_method_name_list[i])
        
        data = {
            "tokens": case_and_method_name_list
        }

        response = requests.post(url, headers=headers, json=data)
        if response.status_code == 200:
            embedding_list = response.json()["embeddings"]
            caseName_embedding = embedding_list[0] # case Name embedding is a list , we need to build it into one sentence with space
            method_embedding_list = embedding_list[1:]
            similarity_list = []
            for method_embedding in method_embedding_list:
                similarity_list.append(self.cosine_similarity(caseName_embedding,method_embedding))
            return similarity_list
        else:
            logging.error(f"Error: Unable to fetch the embedding. Status code: {response.status_code}")




    def download_SO_word2vec(self):
        '''
        Download the Stack Over Flow Word2Vec model from the ZENODO
        '''
        # URL of the file to download
        url = "https://zenodo.org/record/1199620/files/SO_vectors_200.bin?download=1"

        # Send a GET request to download the file
        response = requests.get(url, stream=True)

        # Check if the request was successful
        if response.status_code == 200:
            # Define the path where the file will be saved
            file_path = 'SO_vectors_200.bin'

            # Get the total file size from the headers
            total_size = int(response.headers.get('content-length', 0))

            # Open the file in binary write mode
            with open(file_path, 'wb') as file:
                # Use tqdm to show progress bar
                for data in tqdm.tqdm(response.iter_content(1024), total=total_size//1024, unit='KB', desc="Downloading"):
                    file.write(data)

            logging.info("File downloaded successfully.")
        else:
            # Log an error message if the download failed
            logging.error(f"Error: Unable to download the file. Status code: {response.status_code}")

if __name__ == "__main__":
    # test the nameSimilarity class
    testList = ["NEW org.apache.commons.cli.CommandLine.Builder()","org.apache.commons.cli.CommandLine.Builder.addArg(String)","org.apache.commons.cli.CommandLine.Builder.build()","org.apache.commons.cli.Option.builder(String)"]
    caseList = ["testBuilder","testBuilder","testBuilder","testBuilder"]
    ns = NameSimilarity(word2vec_model='CB')
    print(ns.genNameSimilarity(testList,caseList))
    #TODO: Here maybe some problem when the method from differet class but has the same name.
    # WE should consider add the class name into the method name. MAYBE HELPFUL
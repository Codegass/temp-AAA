import re
import logging

#TODO: Issue: test keyword is not removed
class WordCleaning():
    '''
    Clean the word by removing the special character and lower case the word.
    '''
    def __init__(self):
        self.stopwords = [""]

    def cleanWord(self, word:str) -> list:
        '''
        Clean the word by removing the special character and lower case the word.
        '''
        return self.potential_method_name_process(word)

    def remove_null(self, strlist):
        result_list = []
        for _ in strlist:
            if _ != '':
                result_list.append(_)
        return result_list

    def remove_all_punctuantion(self, method):
        if isinstance(method, str):
            return method.split('_')
        elif isinstance(method, list):
            result_list = []
            for _ in method:
                result_list = result_list + _.split('_')
            return self.remove_null(result_list)

    def convert_2_to_to(self, method):
        if isinstance(method, str):
            return method.replace('2','to')
        elif isinstance(method, list):
            result_list = list()
            for _ in method:
                result_list.append(_.replace('2','to'))
            return self.remove_null(result_list)

    def remove_word_test(self, method):
        if isinstance(method, str):
            return self.remove_null(method.split('test'))
        elif isinstance(method, list):
            result_list = list()
            for _ in method:
                result_list = result_list + _.split('test')
            return self.remove_null(result_list)

    def split_with_upper_case(self, method):
        #TODO: 定义了 大写连写 鉴定拼凑函数，比如 URL

        if isinstance(method, str):
            result_list = re.sub( r"([A-Z]+)", r" \1", method).split()
            return result_list
            
        if isinstance(method, list):
            result_list = list()
            for _ in method:
                result_list = result_list + re.sub( r"([A-Z]+)", r" \1", _).split()
            return self.remove_null(result_list)
    
    def lower_case(self, method):
        if isinstance(method, str):
            return method.lower()
        elif isinstance(method, list):
            result_list = list()
            for _ in method:
                result_list.append(_.lower())
            return self.remove_null(result_list)

    def remove_all_stop_words(self, method):
        if isinstance(method, str):
            return method
        elif isinstance(method, list):
            filtered_words = [word for word in method if word not in self.stopwords]
            return filtered_words

    def IT_split(self,method):
        if isinstance(method, str):
            result_list = re.sub( r"(IT)", r" \1 ", method)
            result_list = re.sub(r"(VM)", r" \1 ", result_list).split()
            return result_list
        if isinstance(method, list):
            result_list = list()
            for _ in method:
                item = re.sub( r"(IT)", r" \1 ", _)
                item = re.sub(r"(VM)", r" \1 ", item)
                result_list = result_list + item.split()
            return self.remove_null(result_list)

    def specific_twist(self, method):
        if isinstance(method, str):
            result_list = re.sub( r"(\d+)", r" \1 ", method).split()
            return result_list
        if isinstance(method, list):
            result_list = list()
            for _ in method:
                result_list = result_list + re.sub( r"(\d+)", r" \1 ", _).split()
            return self.remove_null(result_list)

    def potential_method_name_process(self,method):
        
        try: 
            short_method_name = re.findall(r"(\w+)\(", method)[0]
        except Exception as e:
            try:
                str_method = str(method)
                short_method_name = re.findall(r"(\w+)\<", str_method)[0]
            except Exception as e2:
                short_method_name = method
                # print(method)
                # print(e2)
            
        return self.specific_twist(self.remove_all_stop_words(self.lower_case(self.remove_word_test(self.remove_all_punctuantion(self.IT_split(self.split_with_upper_case(short_method_name)))))))

if __name__ == "__main__":
    #test the wordCleaning class
    wc = WordCleaning()
    print(wc.cleanWord("org.apache.commons.cli.Option.builder(String)"))
    print(wc.cleanWord("GET org.apache.commons.cli.CommandLine.getArgs()"))
#!/usr/bin/python

import sys
import numpy.random
import theano
import theano.tensor as T
import mlp
from logistic_sgd import LogisticRegression, load_data, shared_dataset

class Resampler:
    """
    Resample a dataset either uniformly or with a given probability
    distribution
    """

    def __init__(self,dataset):
        self.train,self.valid,self.test = dataset
        self.train_x, self.train_y = self.train
        self.valid_x, self.valid_y = self.valid
        self.test_x, self.test_y = self.test
        self.train_size = len(self.train_x)
        
    def make_new_train(self,sample_size,distribution=None):
        if distribution is None:
            sample = numpy.random.randint(low=0,high=self.train_size,size=sample_size)
        else:
            raise Exception("not implemented");
        sampled_x = []
        sampled_y = []
        for s in sample:
            sampled_x.append(self.train_x[s])
            sampled_y.append(self.train_y[s])
        return shared_dataset((sampled_x,sampled_y))

    def get_valid(self):
        return shared_dataset(self.valid)

    def get_test(self):
        return shared_dataset(self.test)

if __name__ == '__main__':
    learning_rate=0.01
    L1_reg=0.00
    L2_reg=0.00
    n_epochs=1000
    dataset='mnist.pkl.gz'
    batch_size=100
    resample_size=50000
    n_hidden=[(2500,0.5,'h0',T.tanh),
              (2000,0.5,'h1',T.tanh),
              (1500,0.5,'h2',T.tanh),
              (1000,0.5,'h2',T.tanh),
              (500,0.5,'h3',T.tanh)
             ]
    ensemble_size = 3
    for arg in sys.argv[1:]:
        if arg[0]=='-':
            exec(arg[1:])
    dataset = load_data(dataset,shared=False)
    resampler = Resampler(dataset)
    members = []
    for i in range(0,ensemble_size):
        print 'training member {0}'.format(i)
        mlp=mlp.train_and_select(resampler.make_new_train(resample_size),
                resampler.get_valid(),learning_rate, L1_reg, L2_reg, n_epochs,
                dataset, batch_size, n_hidden, update_rule = mlp.rprop)
        members.append(mlp)

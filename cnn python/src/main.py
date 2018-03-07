import os
from os import path
import numpy as np
from datetime import datetime
from keras.utils import np_utils
from sklearn.model_selection import StratifiedKFold
from sklearn.preprocessing import LabelEncoder
import NeuralNetDefinition
import Parser
import itertools
import copy

dataset_path = r'D:\Datasets\AI-GT'
dataset_path = path.join(dataset_path, 'RUSH_RND2')
train_path = path.join(dataset_path, 'random')
test_path = path.join(dataset_path, 'structured')
Parser.set_unit_onehot_in_map = Parser.ignore_enemy_setter
Parser.min_units_for_player = 1
epochs, batch_size = 6, 5


def get_configuration(train_size, test_size):
    def get_name(p):
        return p[p.rfind('\\') + 1:]

    conf = list()
    title = '-----configuration:-----\n'
    conf.append(title)
    conf.append('Training Configuration:')
    conf.append('train: {} ({} samples)'.format(get_name(train_path), train_size))
    conf.append('test: {} ({} samples)'.format(get_name(test_path), test_size))
    conf.append('minimum units for profiled player: {}'.format(Parser.min_units_for_player))
    conf.append('onehot style: {}'.format(Parser.set_unit_onehot_in_map.__name__))
    conf.append('epochs: {}'.format(epochs))
    conf.append('batch size: {}'.format(batch_size))

    conf.append('\nData Configuration:')
    with open(path.join(dataset_path, 'conf.txt'), 'r') as conf_file:
        conf.append(conf_file.read())

    conf.append('-' * len(title))
    return '\n'.join(conf)


def generate_report(conf, metrics_names, metrics_values):
    report = list()

    for name, val in zip(metrics_names, metrics_values):
        report.append('{name} = {val}'.format(name=name, val=val))

    report.append('\n')

    report.append(conf)

    return '\n'.join(report)


def make_label_encoder():
    Y = os.listdir(train_path)
    nb_labels = len(Y)

    encoder = LabelEncoder()
    encoder.fit(Y)

    def encode_to_onehot(S):
        return np_utils.to_categorical(encoder.transform(S))

    return encode_to_onehot, nb_labels


def load_dataset(dataset_dir_path, label_encoder):
    X, Y = [], []
    for x, y in Parser.iter_samples(dataset_dir_path):
        X.append(x)
        Y.append(y)

    X = np.array(X)
    Y = label_encoder(Y)
    return X, Y


def run_experiment():
    conf = get_configuration('?', '?')
    print(conf)

    label_encoder, nb_labels = make_label_encoder()

    print('*loading train data')
    X_train, y_train = load_dataset(train_path, label_encoder)
    train_size = len(X_train)

    print('*generating cnn')
    cnn = NeuralNetDefinition.generate_cnn(X_train[0].shape, nb_labels)

    print('*training')
    cnn.fit(X_train, y_train, batch_size=batch_size, epochs=epochs, verbose=2, shuffle=True)

    print('*cleaning up memory...')
    del X_train
    del y_train

    print('*loading test data')
    X_test, y_test = load_dataset(test_path, label_encoder)
    test_size = len(X_test)
    results = cnn.evaluate(X_test, y_test, 10)

    print('*testing')
    conf = get_configuration(train_size, test_size)
    report = generate_report(conf, cnn.metrics_names, results)
    print(report)
    with open(path.join(dataset_path, 'results report ({}).txt'.format(datetime.now().strftime("%I.%M%p on %B %d, %Y"))), 'w') as report_file:
        report_file.write(report)

    print('*done')
    return results


if __name__ == '__main__':
    print("Starting: " + datetime.now().strftime("%I:%M%p on %B %d, %Y"))
    for i in range(6):
        Parser.min_units_for_player = i
        Parser.set_unit_onehot_in_map = Parser.indicate_player_setter
        run_experiment()
        Parser.set_unit_onehot_in_map = Parser.ignore_enemy_setter
        run_experiment()
    # run_experiment()

from keras import Sequential
from keras.layers import Activation, Dense, Dropout, Flatten, Convolution2D, MaxPooling2D

pool_size = (4, 4)
nb_filters = 10


def generate_cnn(input_shape, output_dim):
    model = Sequential()
    model.add(Convolution2D(
        nb_filters,
        kernel_size=(16, 16),
        input_shape=input_shape,
        data_format="channels_last",
        activation='relu'
    ))

    model.add(MaxPooling2D(pool_size=pool_size))

    # model.add(Dropout(0.5))

    model.add(Flatten())

    model.add(Dense(128, activation='relu', kernel_initializer="normal"))

    # model.add(Dropout(0.5))

    model.add(Dense(output_dim, kernel_initializer="normal"))

    model.add(Activation('softmax'))

    model.compile(loss='categorical_crossentropy', optimizer='adam', metrics=['mse', 'accuracy'])

    return model

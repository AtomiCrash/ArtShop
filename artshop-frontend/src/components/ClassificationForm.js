import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { TextField, Button, Box, Snackbar } from '@mui/material';
import { createClassification, updateClassification, getClassificationById } from '../api';

function ClassificationForm() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [classification, setClassification] = useState({ name: '', description: '' });
    const [snackbarOpen, setSnackbarOpen] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState('');

    useEffect(() => {
        if (id) {
            fetchClassification();
        }
    }, [id]);

    const fetchClassification = async () => {
        try {
            const response = await getClassificationById(id);
            setClassification(response.data);
        } catch (error) {
            const errorMessage = error.response?.data?.message || 'Неизвестная ошибка при загрузке классификации';
            setSnackbarMessage(errorMessage);
            setSnackbarOpen(true);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const classificationData = { name: classification.name, description: classification.description };
            if (id) {
                await updateClassification(id, classificationData);
                setSnackbarMessage('Классификация успешно обновлена');
            } else {
                await createClassification(classificationData);
                setSnackbarMessage('Классификация успешно добавлена');
            }
            setSnackbarOpen(true);
            navigate('/classifications');
            window.location.reload();
        } catch (error) {
            const errorMessage = error.response?.data?.message || 'Неизвестная ошибка при сохранении классификации';
            setSnackbarMessage(errorMessage);
            setSnackbarOpen(true);
        }
    };

    const handleCloseSnackbar = () => {
        setSnackbarOpen(false);
    };

    return (
        <div>
            <h2>{id ? 'Редактировать классификацию' : 'Добавить классификацию'}</h2>
            <form onSubmit={handleSubmit}>
                <Box mb={2}>
                    <TextField
                        label="Название"
                        value={classification.name || ''}
                        onChange={(e) => setClassification({ ...classification, name: e.target.value })}
                        fullWidth
                        required
                    />
                </Box>
                <Box mb={2}>
                    <TextField
                        label="Описание"
                        value={classification.description || ''}
                        onChange={(e) => setClassification({ ...classification, description: e.target.value })}
                        fullWidth
                        multiline
                        rows={4}
                    />
                </Box>
                <Button type="submit" variant="contained">Сохранить</Button>
                <Button variant="outlined" onClick={() => navigate('/classifications')}>Отмена</Button>
            </form>
            <Snackbar
                open={snackbarOpen}
                autoHideDuration={6000}
                onClose={handleCloseSnackbar}
                message={snackbarMessage}
            />
        </div>
    );
}

export default ClassificationForm;
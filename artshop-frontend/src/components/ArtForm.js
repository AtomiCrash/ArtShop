import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { TextField, Button, Box, Autocomplete, Snackbar } from '@mui/material';
import { createArt, updateArt, getArtById, getAllArtists, getAllClassifications } from '../api';

function ArtForm() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [art, setArt] = useState({ title: '', year: '', classificationId: null, artistIds: [] });
    const [artists, setArtists] = useState([]);
    const [classifications, setClassifications] = useState([]);
    const [snackbarOpen, setSnackbarOpen] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState('');

    const fetchArt = useCallback(async () => {
        if (!id) return;
        try {
            const response = await getArtById(id);
            console.log('Fetched art data:', response.data); // Отладка
            setArt({
                title: response.data.title || '',
                year: response.data.year || '',
                classificationId: response.data.classification?.id || null,
                artistIds: response.data.artists?.map(a => a.id) || []
            });
        } catch (error) {
            console.error('Error fetching art:', error);
            setSnackbarMessage('Не удалось загрузить данные произведения: ' + (error.response?.data?.message || error.message));
            setSnackbarOpen(true);
        }
    }, [id]); // Зависимости: id

    useEffect(() => {
        fetchData();
        if (id) {
            fetchArt();
        }
    }, [id, fetchArt]); // Зависимости включают fetchArt

    const fetchData = async () => {
        try {
            const [artistsRes, classificationsRes] = await Promise.all([getAllArtists(), getAllClassifications()]);
            setArtists(artistsRes.data);
            setClassifications(classificationsRes.data);
        } catch (error) {
            const errorMessage = error.response?.data?.message || 'Неизвестная ошибка при загрузке данных';
            setSnackbarMessage(errorMessage);
            setSnackbarOpen(true);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const currentYear = new Date().getFullYear();
            if (art.year && parseInt(art.year) > currentYear) {
                setSnackbarMessage(`Год не может быть больше ${currentYear}`);
                setSnackbarOpen(true);
                return;
            }

            const selectedClassification = classifications.find(c => c.id === art.classificationId);
            const selectedArtists = artists.filter(a => art.artistIds.includes(a.id));

            const artData = {
                title: art.title.trim(),
                year: art.year ? parseInt(art.year) : null,
                classification: selectedClassification ? {
                    id: selectedClassification.id,
                    name: selectedClassification.name,
                    description: selectedClassification.description || 'No description'
                } : null,
                artists: selectedArtists.map(a => ({
                    id: a.id,
                    firstName: a.firstName || '',
                    lastName: a.lastName || ''
                }))
            };
            let response;
            if (id) {
                response = await updateArt(id, artData);
                setSnackbarMessage('Произведение успешно обновлено');
            } else {
                response = await createArt(artData);
                setSnackbarMessage('Произведение успешно добавлено');
            }
            setSnackbarOpen(true);
            if (response.data) {
                setArt({
                    title: response.data.title || '',
                    year: response.data.year || '',
                    classificationId: response.data.classification?.id || null,
                    artistIds: response.data.artists?.map(a => a.id) || []
                });
            }
            navigate('/arts');
            window.location.reload();
        } catch (error) {
            console.error('Error details:', error.response?.data || error.message);
            const errorMessage = error.response?.data?.message || 'Неизвестная ошибка при сохранении произведения';
            setSnackbarMessage(errorMessage);
            setSnackbarOpen(true);
        }
    };

    const handleCloseSnackbar = () => {
        setSnackbarOpen(false);
    };

    return (
        <div>
            <h2>{id ? 'Редактировать произведение' : 'Добавить произведение'}</h2>
            <form onSubmit={handleSubmit}>
                <Box mb={2}>
                    <TextField
                        label="Название"
                        value={art.title || ''}
                        onChange={(e) => setArt({ ...art, title: e.target.value })}
                        fullWidth
                        required
                        error={!art.title.trim()}
                        helperText={!art.title.trim() ? 'Название обязательно' : ''}
                    />
                </Box>
                <Box mb={2}>
                    <TextField
                        label="Год"
                        type="number"
                        value={art.year || ''}
                        onChange={(e) => setArt({ ...art, year: e.target.value })}
                        fullWidth
                        required
                        error={art.year && parseInt(art.year) > new Date().getFullYear()}
                        helperText={art.year && parseInt(art.year) > new Date().getFullYear() ? 'Год не может быть в будущем' : ''}
                    />
                </Box>
                <Box mb={2}>
                    <Autocomplete
                        options={classifications}
                        getOptionLabel={(option) => option.name}
                        value={classifications.find(c => c.id === art.classificationId) || null}
                        onChange={(e, value) => setArt({ ...art, classificationId: value?.id || null })}
                        renderInput={(params) => <TextField {...params} label="Классификация" />}
                    />
                </Box>
                <Box mb={2}>
                    <Autocomplete
                        multiple
                        options={artists}
                        getOptionLabel={(option) => `${option.firstName} ${option.lastName}`}
                        value={artists.filter(a => art.artistIds.includes(a.id))}
                        onChange={(e, value) => setArt({ ...art, artistIds: value.map(v => v.id) })}
                        renderInput={(params) => <TextField {...params} label="Артисты" />}
                    />
                </Box>
                <Button type="submit" variant="contained" disabled={!art.title.trim() || !art.year}>Сохранить</Button>
                <Button variant="outlined" onClick={() => navigate('/arts')}>Отмена</Button>
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

export default ArtForm;
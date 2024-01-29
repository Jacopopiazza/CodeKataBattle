package com.polimi.PPP.CodeKataBattle.service;

import com.polimi.PPP.CodeKataBattle.DTOs.SubmissionDTO;
import com.polimi.PPP.CodeKataBattle.Exceptions.InvalidArgumentException;
import com.polimi.PPP.CodeKataBattle.Exceptions.InvalidBattleStateException;
import com.polimi.PPP.CodeKataBattle.Exceptions.UserNotSubscribedException;
import com.polimi.PPP.CodeKataBattle.Model.*;
import com.polimi.PPP.CodeKataBattle.Repositories.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private BattleSubscriptionRepository battleSubscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BattleScoreRepository battleScoreRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public SubmissionDTO createSubmission(Long battleId, Long userId, String repositoryUrl, String commitHash) throws InvalidBattleStateException, UserNotSubscribedException {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new EntityNotFoundException("Battle not found"));

        if (battle.getState() != BattleStateEnum.ONGOING) {
            throw new InvalidBattleStateException("Battle is not in the ONGOING state.");
        }

        boolean isSubscribed = battleSubscriptionRepository.existsByBattleIdAndUserId(battleId, userId);
        if (!isSubscribed) {
            throw new UserNotSubscribedException("User is not subscribed to this battle.");
        }

        Submission submission = new Submission();
        submission.setTimestamp(new Timestamp(System.currentTimeMillis()));
        submission.setState(SubmissionStateEnum.PENDING);
        submission.setRepositoryUrl(repositoryUrl);
        submission.setCommitHash(commitHash);

        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        submission.setUser(user);
        submission.setBattle(battle);

        Submission result = submissionRepository.save(submission);

        SubmissionDTO submissionDTO = new SubmissionDTO();
        modelMapper.map(result, submissionDTO);

        return submissionDTO;
    }

    @Transactional
    public void createSubmissionScore(Long submissionId, SubmissionStateEnum state, Integer automaticScore, String log) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new InvalidArgumentException("Submission not found"));

        if (submission.getState() != SubmissionStateEnum.PENDING) {
            throw new InvalidBattleStateException("Submission is not in the PENDING state.");
        }

        submission.setState(state);

        BattleScore battleScore = new BattleScore();
        battleScore.setAutomaticScore(automaticScore);
        battleScore.setLogScoring(log);
        battleScore.setSubmission(submission);

        submissionRepository.save(submission);
        battleScoreRepository.save(battleScore);

    }

    public List<SubmissionDTO> getPendingSubmissions() {

        return submissionRepository.findAllByState(SubmissionStateEnum.PENDING).stream().map(submission -> {
            SubmissionDTO submissionDTO = new SubmissionDTO();
            modelMapper.map(submission, submissionDTO);
            return submissionDTO;
        }).toList();
    }



}
